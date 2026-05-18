// ============================================================
//  JUEGO DE CALIBRACION CON POTENCIOMETRO
// ============================================================

#include <SPI.h>
#include <MFRC522.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// --- CONFIGURACIÓN RED Y MQTT ---
const char* WIFI_SSID      = "Nothing";
const char* WIFI_PASS      = "joan2002";

const char* MQTT_HOST      = "129.158.197.45";
const int   MQTT_PORT      = 1883;
const char* MQTT_USER      = "pirata";
const char* MQTT_PASS_STR  = "oro123";
const char* MQTT_CLIENT_ID = "esp32-potenciometro";

const char* EVENTO_INICIO     = "started";
const char* EVENTO_COMPLETADO = "completed";

// --- PINES RFID  ---
#define RST_PIN   13 
#define SS_PIN    19 
#define SCK_PIN   18
#define MISO_PIN  16
#define MOSI_PIN  17

// --- PINES JUEGO ---
const int POT_PIN = 34; 
const int LED_R   = 14;   
const int LED_Y   = 26;   
const int LED_G   = 27;   

// --- INSTANCIAS ---
MFRC522 rfid(SS_PIN, RST_PIN);
MFRC522::MIFARE_Key claveNDEF; // Llave para móvil
MFRC522::MIFARE_Key claveFABRICA; // Llave por defecto

WiFiClient   wifiClient;
PubSubClient mqtt(wifiClient);

// --- VARIABLES DE ESTADO Y RED ---
bool   sistemaAutorizado = false;
String playerID = "";
String joinCode = "";

// --- CONFIGURACIÓN DE DIFICULTAD JUEGO ---
int objetivoActual = 0;
const int RONDAS_TOTALES = 5; 
int rondasCompletadas = 0;

const int ESCALA_MAX = 3000;    
const int MARGEN_VERDE = 40;    
const int MARGEN_AMARILLO = 400; 
const int TIEMPO_A_AGUANTAR = 5000; 
unsigned long tiempoEnZona = 0;
bool partidaGanada = false;

// --- TIEMPO LÍMITE ---
const unsigned long TIEMPO_LIMITE = 120000; // 2 minutos 
unsigned long tiempoInicio = 0;

// --- FILTRO DE VISCOSIDAD ---
float valorSuave = 0;
const float factorFiltro = 0.03; 

// ============================================================
//  PAYLOAD MQTT
// ============================================================
String buildPayload(const char* evento, String player, String joinCode) {
  StaticJsonDocument<256> doc;
  doc["mission_id"] = 1;
  doc["username"] = player;
  String out;
  serializeJson(doc, out);
  return out;
}

// ============================================================
//  SETUP
// ============================================================
void setup() {
  Serial.begin(115200);
  pinMode(LED_R, OUTPUT);
  pinMode(LED_Y, OUTPUT);
  pinMode(LED_G, OUTPUT);

  // Inicialización SPI con pines personalizados
  SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
  rfid.PCD_Init();

  // Semilla para las rondas aleatorias
  randomSeed(analogRead(32));

  // Llave estándar que usa NFC Tools para NDEF
  byte keyNdefBytes[] = { 0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7 };
  for (byte i = 0; i < 6; i++) {
    claveNDEF.keyByte[i] = keyNdefBytes[i];
    claveFABRICA.keyByte[i] = 0xFF;
  }

  conectarWiFi();
  mqtt.setServer(MQTT_HOST, MQTT_PORT);
  
  Serial.println("\n=== SISTEMA BLOQUEADO (Compatible con Móvil NDEF) ===");
  Serial.println("Por favor, escanea tu tarjeta o móvil...");
}

// ============================================================
//  LOOP PRINCIPAL
// ============================================================
void loop() {
  verificarWiFi();
  mantenerMQTT();

  // Fase 1: Esperar autorización por RFID
  if (!sistemaAutorizado) {
    esperarTarjeta();
    return;
  }

  // Fase 3: Victoria 
  if (partidaGanada) {
    digitalWrite(LED_G, HIGH); delay(1000);
    digitalWrite(LED_G, LOW); delay(500);
    return; 
  }

  // ---CONTROL DE TIEMPO LÍMITE (2 MINUTOS) ---
  if (millis() - tiempoInicio >= TIEMPO_LIMITE) {
    Serial.println("\n⏳ ¡TIEMPO AGOTADO! Volviendo al inicio...");
    
    // Parpadeo rojo rápido para indicar fallo de tiempo
    for(int i=0; i<4; i++){ 
      digitalWrite(LED_R, HIGH); delay(150); 
      digitalWrite(LED_R, LOW); delay(150); 
    }
    
    // Apagamos todos los LEDs y reseteamos variables clave
    digitalWrite(LED_Y, LOW); 
    digitalWrite(LED_G, LOW);
    sistemaAutorizado = false;
    rondasCompletadas = 0;
    tiempoEnZona = 0;
    
    Serial.println("\n=== SISTEMA BLOQUEADO ===");
    Serial.println("Por favor, escanea tu tarjeta o móvil...");
    return; // Corta el loop aquí para volver a esperar tarjeta
  }

  // Fase 2: Lógica del Juego
  int lectura = analogRead(POT_PIN);
  int valorMapeado = map(lectura, 0, 4095, 0, ESCALA_MAX);
  
  valorSuave = (valorSuave * (1.0 - factorFiltro)) + (valorMapeado * factorFiltro);
  int valorFinal = (int)valorSuave;
  int distancia = abs(valorFinal - objetivoActual);

  // Monitorización
  static unsigned long lastPrint = 0;
  if (millis() - lastPrint > 200) { 
    Serial.print("Ronda: "); Serial.print(rondasCompletadas + 1);
    Serial.print(" | Objetivo: "); Serial.print(objetivoActual);
    Serial.print(" | Dial: "); Serial.println(valorFinal);
    lastPrint = millis();
  }

  if (distancia <= MARGEN_VERDE) {
    digitalWrite(LED_G, HIGH); digitalWrite(LED_Y, LOW); digitalWrite(LED_R, LOW);
    if (tiempoEnZona == 0) tiempoEnZona = millis();
    if (millis() - tiempoEnZona > TIEMPO_A_AGUANTAR) faseCompletada();
  } 
  else if (distancia <= MARGEN_AMARILLO) {
    digitalWrite(LED_G, LOW); digitalWrite(LED_Y, HIGH); digitalWrite(LED_R, LOW);
    tiempoEnZona = 0;
  } 
  else {
    digitalWrite(LED_G, LOW); digitalWrite(LED_Y, LOW); digitalWrite(LED_R, HIGH);
    tiempoEnZona = 0;
  }
  delay(20); 
}

// ============================================================
//  FUNCIONES DEL JUEGO
// ============================================================
void protocoloEncendido() {
  Serial.println(">>> INICIANDO SISTEMA DE NÚCLEO...");
  for(int i=0; i<3; i++) {
    digitalWrite(LED_R, HIGH); delay(400);
    digitalWrite(LED_R, LOW); digitalWrite(LED_Y, HIGH); delay(400);
    digitalWrite(LED_Y, LOW); digitalWrite(LED_G, HIGH); delay(400);
    digitalWrite(LED_G, LOW);
  }
  delay(1000);
}

void iniciarNuevaRonda() {
  int nuevo;
  do {
    nuevo = random(200, 2800);
  } while (abs(nuevo - (int)valorSuave) < 600); 

  objetivoActual = nuevo;
  tiempoEnZona = 0;
  
  for(int i=0; i<5; i++) {
    digitalWrite(LED_Y, HIGH); delay(150);
    digitalWrite(LED_Y, LOW); delay(150);
  }
}

void faseCompletada() {
  rondasCompletadas++;
  Serial.println(">>> SECTOR FIJADO.");

  for(int i=0; i<10; i++) {
    digitalWrite(LED_G, HIGH); delay(150);
    digitalWrite(LED_G, LOW); delay(150);
  }

  if (rondasCompletadas >= RONDAS_TOTALES) {
    victoriaFinal();
  } else {
    iniciarNuevaRonda();
  }
}

void victoriaFinal() {
  partidaGanada = true;
  Serial.println(">>> SISTEMA COMPLETADO. CALIBRACIÓN TOTAL.");
  publicarEvento(EVENTO_COMPLETADO); // Avisamos al servidor que ha ganado
}

// ============================================================
//  FUNCIONES RFID / NDEF
// ============================================================
String leerMensajeCompleto() {
  String mensajeTotal = "";
  byte bloquesParaLeer[] = {4, 5}; 

  for (byte i = 0; i < 2; i++) {
    byte bloqueActual = bloquesParaLeer[i];
   
    MFRC522::StatusCode status = rfid.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, bloqueActual, &claveNDEF, &(rfid.uid));
    if (status != MFRC522::STATUS_OK) {
      status = rfid.PCD_Authenticate(MFRC522::PICC_CMD_MF_AUTH_KEY_A, bloqueActual, &claveFABRICA, &(rfid.uid));
    }

    if (status == MFRC522::STATUS_OK) {
      byte buffer[18], size = sizeof(buffer);
      if (rfid.MIFARE_Read(bloqueActual, buffer, &size) == MFRC522::STATUS_OK) {
        for (byte j = 0; j < 16; j++) {
          if (buffer[j] >= 32 && buffer[j] <= 126) { 
            mensajeTotal += (char)buffer[j];
          }
        }
      }
    }
  }
  return mensajeTotal;
}

bool parsearDatos(String raw) {
  int posicionComa = raw.indexOf(',');
  if (posicionComa == -1) return false;

  String playerExtraido = "";
  for (int i = posicionComa - 1; i >= 0; i--) {
    char c = raw[i];
    if (i < posicionComa - 1 && islower(raw[i]) && islower(raw[i+1]) && i < 3) break;
    playerExtraido = c + playerExtraido;
  }

  String codeExtraido = raw.substring(posicionComa + 1);

  playerID = playerExtraido;
  playerID.trim();
 
  joinCode = "";
  for(int i=0; i < codeExtraido.length(); i++){
    if(isalnum(codeExtraido[i])) joinCode += codeExtraido[i];
    if(joinCode.length() >= 6) break; 
  }

  if (playerID.length() > 0 && joinCode.length() > 0) {
    if (playerID.startsWith("en")) playerID = playerID.substring(2);
    return true;
  }
 
  return false;
}

void esperarTarjeta() {
  if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) return;

  Serial.println("Dispositivo detectado. Leyendo datos...");
 
  String raw = leerMensajeCompleto();
  Serial.print("RAW total: "); Serial.println(raw); 

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  if (parsearDatos(raw)) {
    Serial.println("✅ ACCESO CONCEDIDO -> ID: " + playerID + " | Code: " + joinCode);
    publicarEvento(EVENTO_INICIO);
    sistemaAutorizado = true; // Activa la bandera para pasar a la fase del juego
    rondasCompletadas = 0;    // Nos aseguramos de limpiar rondas pasadas
    tiempoInicio = millis();  // <--- INICIAMOS EL CRONÓMETRO AQUÍ
    protocoloEncendido(); 
    iniciarNuevaRonda();
  } else {
    Serial.println("❌ Error en formato o lectura incompleta");
    for(int i=0; i<3; i++){ digitalWrite(LED_R, HIGH); delay(200); digitalWrite(LED_R, LOW); delay(200); }
  }
}

// ============================================================
//  FUNCIONES RED (WIFI / MQTT)
// ============================================================
void conectarWiFi() {
  Serial.print("Conectando WiFi...");
  WiFi.begin(WIFI_SSID, WIFI_PASS);
  while (WiFi.status() != WL_CONNECTED) { delay(500); Serial.print("."); }
  Serial.println(" OK");
}

void verificarWiFi() { 
  if (WiFi.status() != WL_CONNECTED) conectarWiFi(); 
}

void mantenerMQTT() {
  if (!mqtt.connected()) {
    if (mqtt.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS_STR)) {
      Serial.println("MQTT Reconectado");
    } else {
      delay(2000);
    }
  }
  mqtt.loop();
}

void publicarEvento(const char* evento) {
  String topic = "juego/devices/" + joinCode + "/JUEGOPOT/" + evento;
  String payload = buildPayload(evento, playerID, joinCode);
  mqtt.publish(topic.c_str(), payload.c_str());
  Serial.println("Mensaje MQTT enviado: " + String(evento));
}