// ============================================================
//  RFID (NDEF MOBILE READY) + JUEGO ATRAPA LA LUZ + MQTT
// =====================================================
#include <SPI.h>
#include <cstring>
#include <MFRC522.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// --- CONFIGURACIÓN ---
const char* WIFI_SSID      = "Nothing";
const char* WIFI_PASS      = "joan2002";

const char* MQTT_HOST      = "129.158.197.45";
const int   MQTT_PORT      = 1883;
const char* MQTT_USER      = "pirata";
const char* MQTT_PASS_STR  = "oro123";
const char* MQTT_CLIENT_ID = "esp32-juego-01";

const char* EVENTO_INICIO     = "started";
const char* EVENTO_COMPLETADO = "completed";

int velocidad = 500;
const byte BLOQUE_DATOS = 4;

// ============================================================
//  PAYLOAD MQTT
// ============================================================
String buildPayload(const char* evento, const char* playerUsername, String joinCode) {
  StaticJsonDocument<384> doc;
  doc["mission_id"] = 1;
  doc["username"]   = playerUsername;
  String out;
  serializeJson(doc, out);
  return out;
}

// --- PINES ---
#define SS_PIN  5
#define RST_PIN 22
const int BTN_PIN = 12;
const int LED_R   = 14;
const int LED_G   = 27;

MFRC522 rfid(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key claveNDEF; // Llave para móvil
MFRC522::MIFARE_Key claveFABRICA; // Llave por defecto

WiFiClient   wifiClient;
PubSubClient mqtt(wifiClient);

bool   rfidOk   = false;
String playerID = "";
String joinCode = "";
/** Copia estable del jugador leído por NFC (evita usar String global tras mucho tiempo en loop). */
char playerUsernameBuf[40] = "";

void setup() {
  Serial.begin(115200);
  SPI.begin();
  rfid.PCD_Init();

  pinMode(BTN_PIN, INPUT_PULLUP);
  pinMode(LED_R, OUTPUT);
  pinMode(LED_G, OUTPUT);

  // Llave estándar que usa NFC Tools para NDEF
  byte keyNdefBytes[] = { 0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7 };
  for (byte i = 0; i < 6; i++) {
    claveNDEF.keyByte[i] = keyNdefBytes[i];
    claveFABRICA.keyByte[i] = 0xFF;
  }

  conectarWiFi();
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  Serial.println("\n=== SISTEMA LISTO (Compatible con Móvil) ===");
}

void loop() {
  verificarWiFi();
  mantenerMQTT();
  if (!rfidOk) esperarTarjeta();
  else         juegoLoop();
}

// ============================================================
//  RFID — Adaptado para leer NDEF del Móvil
// ============================================================
// Función para leer dos bloques seguidos (4 y 5) y obtener el mensaje completo
String leerMensajeCompleto() {
  String mensajeTotal = "";
  byte bloquesParaLeer[] = {4, 5}; // Leemos dos bloques para asegurar el mensaje

  for (byte i = 0; i < 2; i++) {
    byte bloqueActual = bloquesParaLeer[i];
    
    // Autenticar cada bloque antes de leer
    MFRC522::StatusCode status = rfid.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, bloqueActual, &claveNDEF, &(rfid.uid));
    if (status != MFRC522::STATUS_OK) {
      status = rfid.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, bloqueActual, &claveFABRICA, &(rfid.uid));
    }

    if (status == MFRC522::STATUS_OK) {
      byte buffer[18], size = sizeof(buffer);
      if (rfid.MIFARE_Read(bloqueActual, buffer, &size) == MFRC522::STATUS_OK) {
        for (byte j = 0; j < 16; j++) {
          if (buffer[j] >= 32 && buffer[j] <= 126) { // Solo ASCII legible
            mensajeTotal += (char)buffer[j];
          }
        }
      }
    }
  }
  return mensajeTotal;
}

bool parsearDatos(String raw) {
  // 1. Buscamos la posición de la coma
  int posicionComa = raw.indexOf(',');
  if (posicionComa == -1) return false;

  // 2. El PLAYER termina en la coma. 
  // Pero ojo: los 3 caracteres justo antes de la coma suelen ser parte del nombre.
  // Vamos a retroceder desde la coma hasta encontrar caracteres "extraños" o el inicio.
  String playerExtraido = "";
  for (int i = posicionComa - 1; i >= 0; i--) {
    char c = raw[i];
    // Si encontramos las letras del código de idioma (como 'en') o basura NDEF, paramos
    if (i < posicionComa - 1 && islower(raw[i]) && islower(raw[i+1]) && i < 3) break; 
    playerExtraido = c + playerExtraido;
  }

  // 3. El JOIN CODE empieza justo después de la coma
  String codeExtraido = raw.substring(posicionComa + 1);

  // Limpieza final de espacios o caracteres invisibles
  playerID = playerExtraido;
  playerID.trim();
  
  // Para el joinCode, tomamos solo los primeros 5-6 caracteres alfanuméricos
  joinCode = "";
  for(int i=0; i < codeExtraido.length(); i++){
    if(isalnum(codeExtraido[i])) joinCode += codeExtraido[i];
    if(joinCode.length() >= 6) break; // Suponiendo códigos de 5 letras
  }

  if (playerID.length() > 0 && joinCode.length() > 0) {
    // Limpieza de emergencia: si el playerID todavía tiene la 'en' del móvil
    if (playerID.startsWith("en")) playerID = playerID.substring(2);
    memset(playerUsernameBuf, 0, sizeof(playerUsernameBuf));
    playerID.toCharArray(playerUsernameBuf, sizeof(playerUsernameBuf));
    return true;
  }
  
  return false;
}


void esperarTarjeta() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return;

  Serial.println("Tarjeta detectada. Leyendo datos...");
  
  String raw = leerMensajeCompleto();
  Serial.print("RAW total: "); Serial.println(raw); // Aquí verás todo el texto unido

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  if (parsearDatos(raw)) {
    Serial.println("✅ OK -> ID: " + playerID + " | Code: " + joinCode);
    publicarEvento(EVENTO_INICIO);
    rfidOk = true;
    digitalWrite(LED_G, HIGH); delay(1000); digitalWrite(LED_G, LOW);
  } else {
    Serial.println("❌ Error en formato o lectura incompleta");
    for(int i=0; i<3; i++){ digitalWrite(LED_R, HIGH); delay(200); digitalWrite(LED_R, LOW); delay(200); }
  }
}

// ============================================================
//  MQTT & WIFI & JUEGO (Igual que tu código original)
// ============================================================
void conectarWiFi() {
  Serial.print("WiFi...");
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  Serial.println(" OK");
}
void verificarWiFi() { if (WiFi.status() != WL_CONNECTED) conectarWiFi(); }
void mantenerMQTT() {
  if (!mqtt.connected()) {
    if (mqtt.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS_STR)) Serial.println("MQTT OK");
    else delay(2000);
  }
  mqtt.loop();
}
void publicarEvento(const char* evento) {
  String topic = "juego/devices/" + joinCode + "/JUEGOLED/" + evento;
  const char* user = (playerUsernameBuf[0] != '\0') ? playerUsernameBuf : playerID.c_str();
  String payload = buildPayload(evento, user, joinCode);
  mqtt.publish(topic.c_str(), payload.c_str());
}
void juegoLoop() {
  digitalWrite(LED_R, HIGH); digitalWrite(LED_G, LOW);
  for (int i = 0; i < velocidad; i++) { if (digitalRead(BTN_PIN) == LOW) { pantallaError(); return; } delay(1); }
  digitalWrite(LED_R, LOW); digitalWrite(LED_G, HIGH);
  for (int i = 0; i < velocidad; i++) { if (digitalRead(BTN_PIN) == LOW) { pantallaVictoria(); return; } delay(1); }
}
void pantallaError() { Serial.println("DERROTA"); for (int i = 0; i < 5; i++) { digitalWrite(LED_R, !digitalRead(LED_R)); delay(150); } rfidOk = false; while(digitalRead(BTN_PIN)==LOW); }
void pantallaVictoria() { Serial.println("VICTORIA"); for (int i = 0; i < 6; i++) { digitalWrite(LED_G, !digitalRead(LED_G)); delay(100); } publicarEvento(EVENTO_COMPLETADO); rfidOk = false; while(digitalRead(BTN_PIN)==LOW); }