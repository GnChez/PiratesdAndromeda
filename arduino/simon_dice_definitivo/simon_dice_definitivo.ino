// ============================================================
//  JUEGO: EL TIMÓN DEL CAPITÁN (SIMON SAYS)
//  VERSIÓN REESTRUCTURADA Y ESTABLE
// ============================================================

#include <SPI.h>
#include <MFRC522.h>
#include <WiFi.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>

// ============================================================
//  CONFIGURACIÓN WIFI / MQTT
// ============================================================

const char* WIFI_SSID      = "Nothing";
const char* WIFI_PASS      = "joan2002";

const char* MQTT_HOST      = "129.158.197.45";
const int   MQTT_PORT      = 1883;

const char* MQTT_USER      = "pirata";
const char* MQTT_PASS_STR  = "oro123";
const char* MQTT_CLIENT_ID = "esp32-simon";

const char* EVENTO_INICIO     = "started";
const char* EVENTO_COMPLETADO = "completed";

// ============================================================
//  PINES RFID
// ============================================================

#define RST_PIN   13
#define SS_PIN    19
#define SCK_PIN   18
#define MISO_PIN  16
#define MOSI_PIN  17

// ============================================================
//  PINES JUEGO
// ============================================================

const int LED_R = 14;
const int LED_Y = 26;
const int LED_G = 27;

const int BTN_R = 32;
const int BTN_Y = 33;
const int BTN_G = 25;

// ============================================================
//  INSTANCIAS
// ============================================================

MFRC522 rfid(SS_PIN, RST_PIN);

MFRC522::MIFARE_Key claveNDEF;
MFRC522::MIFARE_Key claveFABRICA;

WiFiClient wifiClient;
PubSubClient mqtt(wifiClient);

// ============================================================
//  VARIABLES GENERALES
// ============================================================

bool sistemaAutorizado = false;
bool partidaGanada = false;

String playerID = "";
String joinCode = "";

// ============================================================
//  CONFIGURACIÓN JUEGO
// ============================================================

const int RONDAS_TOTALES = 5;

int secuencia[RONDAS_TOTALES];
int rondaActual = 0;
int indiceJugador = 0;

// ============================================================
//  CONTROL TIEMPO
// ============================================================

const unsigned long TIEMPO_LIMITE = 120000;
unsigned long tiempoInicio = 0;

// ============================================================
//  DEBOUNCE BOTONES
// ============================================================

unsigned long ultimoBoton = 0;
const unsigned long DEBOUNCE = 250;

// ============================================================
//  PAYLOAD MQTT
// ============================================================

String buildPayload(const char* evento, String player, String joinCode) {

  StaticJsonDocument<256> doc;

  doc["mission_id"] = 2;
  doc["event"] = evento;
  doc["username"] = player;
  doc["join_code"] = joinCode;

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

  pinMode(BTN_R, INPUT_PULLUP);
  pinMode(BTN_Y, INPUT_PULLUP);
  pinMode(BTN_G, INPUT_PULLUP);

  SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
  rfid.PCD_Init();

  randomSeed(analogRead(35));

  byte keyNdefBytes[] = {0xD3, 0xF7, 0xD3, 0xF7, 0xD3, 0xF7};

  for (byte i = 0; i < 6; i++) {
    claveNDEF.keyByte[i] = keyNdefBytes[i];
    claveFABRICA.keyByte[i] = 0xFF;
  }

  conectarWiFi();

  mqtt.setServer(MQTT_HOST, MQTT_PORT);

  Serial.println("\n=== TIMÓN BLOQUEADO ===");
  Serial.println("Escanea tu tarjeta o móvil...");
}

// ============================================================
//  LOOP PRINCIPAL
// ============================================================

void loop() {

  verificarWiFi();
  mantenerMQTT();

  // ----------------------------------------------------------
  // ESPERAR RFID
  // ----------------------------------------------------------

  if (!sistemaAutorizado) {
    esperarTarjeta();
    return;
  }

  // ----------------------------------------------------------
  // VICTORIA
  // ----------------------------------------------------------

  if (partidaGanada) {

    digitalWrite(LED_G, HIGH);
    delay(500);

    digitalWrite(LED_G, LOW);
    delay(500);

    return;
  }

  // ----------------------------------------------------------
  // TIEMPO LÍMITE
  // ----------------------------------------------------------

  if (millis() - tiempoInicio >= TIEMPO_LIMITE) {

    Serial.println("\n⏳ TIEMPO AGOTADO");

    for (int i = 0; i < 4; i++) {

      digitalWrite(LED_R, HIGH);
      delay(150);

      digitalWrite(LED_R, LOW);
      delay(150);
    }

    resetJuego();
    return;
  }

  // ----------------------------------------------------------
  // LEER BOTONES
  // ----------------------------------------------------------

  int boton = comprobarBotones();

  if (boton != -1) {

    mostrarColor(boton);

    // --------------------------------------
    // CORRECTO
    // --------------------------------------

    if (boton == secuencia[indiceJugador]) {

      indiceJugador++;

      // COMPLETÓ RONDA
      if (indiceJugador > rondaActual) {

        rondaCompletada();
      }
    }

    // --------------------------------------
    // ERROR
    // --------------------------------------

    else {

      errorJugador();
    }
  }

  delay(10);
}

// ============================================================
//  INICIAR PARTIDA
// ============================================================

void iniciarJuego() {

  rondaActual = 0;
  indiceJugador = 0;
  partidaGanada = false;

  generarNuevaRonda();

  protocoloEncendido();

  reproducirSecuencia();
}

// ============================================================
//  GENERAR NUEVA RONDA
// ============================================================

void generarNuevaRonda() {

  secuencia[rondaActual] = random(0, 3);

  indiceJugador = 0;
}

// ============================================================
//  REPRODUCIR SECUENCIA
// ============================================================

void reproducirSecuencia() {

  Serial.print("Ronda ");
  Serial.println(rondaActual + 1);

  delay(700);

  for (int i = 0; i <= rondaActual; i++) {

    mostrarColor(secuencia[i]);

    delay(250);
  }
}

// ============================================================
//  MOSTRAR COLOR
// ============================================================

void mostrarColor(int color) {

  apagarLeds();

  if (color == 0)
    digitalWrite(LED_R, HIGH);

  if (color == 1)
    digitalWrite(LED_Y, HIGH);

  if (color == 2)
    digitalWrite(LED_G, HIGH);

  delay(300);

  apagarLeds();
}

// ============================================================
//  COMPROBAR BOTONES
// ============================================================

int comprobarBotones() {

  if (millis() - ultimoBoton < DEBOUNCE)
    return -1;

  if (digitalRead(BTN_R) == LOW) {

    ultimoBoton = millis();
    return 0;
  }

  if (digitalRead(BTN_Y) == LOW) {

    ultimoBoton = millis();
    return 1;
  }

  if (digitalRead(BTN_G) == LOW) {

    ultimoBoton = millis();
    return 2;
  }

  return -1;
}

// ============================================================
//  RONDA COMPLETADA
// ============================================================

void rondaCompletada() {

  Serial.println(">>> SECUENCIA CORRECTA");

  for (int i = 0; i < 3; i++) {

    digitalWrite(LED_G, HIGH);
    delay(100);

    digitalWrite(LED_G, LOW);
    delay(100);
  }

  rondaActual++;

  // ----------------------------------------------------------
  // VICTORIA FINAL
  // ----------------------------------------------------------

  if (rondaActual >= RONDAS_TOTALES) {

    victoriaFinal();
    return;
  }

  generarNuevaRonda();

  delay(1000);

  reproducirSecuencia();
}

// ============================================================
//  ERROR JUGADOR
// ============================================================

void errorJugador() {

  Serial.println("❌ ERROR DE SECUENCIA");

  for (int i = 0; i < 3; i++) {

    digitalWrite(LED_R, HIGH);
    delay(250);

    digitalWrite(LED_R, LOW);
    delay(250);
  }

  rondaActual = 0;

  generarNuevaRonda();

  delay(1000);

  reproducirSecuencia();
}

// ============================================================
//  VICTORIA FINAL
// ============================================================

void victoriaFinal() {

  partidaGanada = true;

  Serial.println(">>> TIMÓN ASEGURADO");

  publicarEvento(EVENTO_COMPLETADO);
}

// ============================================================
//  RESET COMPLETO
// ============================================================

void resetJuego() {

  apagarLeds();

  sistemaAutorizado = false;
  partidaGanada = false;

  rondaActual = 0;
  indiceJugador = 0;

  Serial.println("\n=== TIMÓN BLOQUEADO ===");
  Serial.println("Escanea tu tarjeta o móvil...");
}

// ============================================================
//  APAGAR LEDS
// ============================================================

void apagarLeds() {

  digitalWrite(LED_R, LOW);
  digitalWrite(LED_Y, LOW);
  digitalWrite(LED_G, LOW);
}

// ============================================================
//  PROTOCOLO ENCENDIDO
// ============================================================

void protocoloEncendido() {

  Serial.println(">>> TIMÓN ACTIVADO");

  for (int i = 0; i < 3; i++) {

    digitalWrite(LED_R, HIGH);
    delay(200);

    digitalWrite(LED_R, LOW);

    digitalWrite(LED_Y, HIGH);
    delay(200);

    digitalWrite(LED_Y, LOW);

    digitalWrite(LED_G, HIGH);
    delay(200);

    digitalWrite(LED_G, LOW);
  }
}

// ============================================================
//  RFID / NDEF
// ============================================================

String leerMensajeCompleto() {

  String mensajeTotal = "";

  byte bloquesParaLeer[] = {4, 5};

  for (byte i = 0; i < 2; i++) {

    byte bloqueActual = bloquesParaLeer[i];

    MFRC522::StatusCode status =
      rfid.PCD_Authenticate(
        MFRC522::PICC_CMD_MF_AUTH_KEY_A,
        bloqueActual,
        &claveNDEF,
        &(rfid.uid)
      );

    if (status != MFRC522::STATUS_OK) {

      status =
        rfid.PCD_Authenticate(
          MFRC522::PICC_CMD_MF_AUTH_KEY_A,
          bloqueActual,
          &claveFABRICA,
          &(rfid.uid)
        );
    }

    if (status == MFRC522::STATUS_OK) {

      byte buffer[18];
      byte size = sizeof(buffer);

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

// ============================================================
//  PARSEAR DATOS
// ============================================================

bool parsearDatos(String raw) {

  int coma = raw.indexOf(',');

  if (coma == -1)
    return false;

  playerID = raw.substring(0, coma);
  joinCode = raw.substring(coma + 1);

  playerID.trim();
  joinCode.trim();

  return playerID.length() > 0 && joinCode.length() > 0;
}

// ============================================================
//  ESPERAR TARJETA
// ============================================================

void esperarTarjeta() {

  if (!rfid.PICC_IsNewCardPresent())
    return;

  if (!rfid.PICC_ReadCardSerial())
    return;

  Serial.println("Dispositivo detectado");

  String raw = leerMensajeCompleto();

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();

  if (parsearDatos(raw)) {

    Serial.println("✅ ACCESO CONCEDIDO");

    publicarEvento(EVENTO_INICIO);

    sistemaAutorizado = true;

    tiempoInicio = millis();

    iniciarJuego();
  }

  else {

    Serial.println("❌ ERROR DE LECTURA");

    for (int i = 0; i < 3; i++) {

      digitalWrite(LED_R, HIGH);
      delay(200);

      digitalWrite(LED_R, LOW);
      delay(200);
    }
  }
}

// ============================================================
//  WIFI
// ============================================================

void conectarWiFi() {

  Serial.print("Conectando WiFi...");

  WiFi.begin(WIFI_SSID, WIFI_PASS);

  while (WiFi.status() != WL_CONNECTED) {

    delay(500);
    Serial.print(".");
  }

  Serial.println(" OK");
}

void verificarWiFi() {

  if (WiFi.status() != WL_CONNECTED) {

    conectarWiFi();
  }
}

// ============================================================
//  MQTT
// ============================================================

void mantenerMQTT() {

  if (!mqtt.connected()) {

    if (mqtt.connect(MQTT_CLIENT_ID, MQTT_USER, MQTT_PASS_STR)) {

      Serial.println("MQTT Reconectado");
    }

    else {

      delay(2000);
    }
  }

  mqtt.loop();
}

void publicarEvento(const char* evento) {

  String topic =
    "juego/devices/" +
    joinCode +
    "/SIMONSAYS/" +
    evento;

  String payload =
    buildPayload(evento, playerID, joinCode);

  mqtt.publish(topic.c_str(), payload.c_str());

  Serial.println("MQTT enviado");
}