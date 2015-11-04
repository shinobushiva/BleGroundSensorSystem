/*
  author Shinobu Izumi (Sojo University)
 */

#define TXRX_BUF_LEN                      20

BLE                                      ble;
Ticker                                    ticker;

//static uint16_t value=0;

// The Nordic UART Service
static const uint8_t service1_uuid[]                = {0x71, 0x3D, 0, 0, 0x50, 0x3E, 0x4C, 0x75, 0xBA, 0x94, 0x31, 0x48, 0xF1, 0x8D, 0x94, 0x1E};
static const uint8_t service1_chars1_uuid[]         = {0x71, 0x3D, 0, 2, 0x50, 0x3E, 0x4C, 0x75, 0xBA, 0x94, 0x31, 0x48, 0xF1, 0x8D, 0x94, 0x1E};
static const uint8_t service1_chars2_uuid[]         = {0x71, 0x3D, 0, 3, 0x50, 0x3E, 0x4C, 0x75, 0xBA, 0x94, 0x31, 0x48, 0xF1, 0x8D, 0x94, 0x1E};
static const uint8_t service1_chars3_uuid[]         = {0x71, 0x3D, 0, 4, 0x50, 0x3E, 0x4C, 0x75, 0xBA, 0x94, 0x31, 0x48, 0xF1, 0x8D, 0x94, 0x1E};
static const uint8_t service1_chars4_uuid[]         = {0x71, 0x3D, 0, 5, 0x50, 0x3E, 0x4C, 0x75, 0xBA, 0x94, 0x31, 0x48, 0xF1, 0x8D, 0x94, 0x1E};
static const uint8_t service1_chars5_uuid[]         = {0x71, 0x3D, 0, 6, 0x50, 0x3E, 0x4C, 0x75, 0xBA, 0x94, 0x31, 0x48, 0xF1, 0x8D, 0x94, 0x1E};
static const uint8_t uart_base_uuid_rev[]           = {0x1E, 0x94, 0x8D, 0xF1, 0x48, 0x31, 0x94, 0xBA, 0x75, 0x4C, 0x3E, 0x50, 0, 0, 0x3D, 0x71};

uint8_t chars1_value[TXRX_BUF_LEN] = {0,};
uint8_t chars2_value[TXRX_BUF_LEN] = {0,};
uint8_t chars3_value[TXRX_BUF_LEN] = {0x01, 0x02, 0x03};
uint8_t chars4_value[TXRX_BUF_LEN] = {0,};
uint8_t chars5_value[TXRX_BUF_LEN] = {0x01, 0x02, 0x03};

GattCharacteristic  characteristic1(service1_chars1_uuid, chars1_value, 1, TXRX_BUF_LEN, GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_WRITE | GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_WRITE_WITHOUT_RESPONSE );
GattCharacteristic  characteristic2(service1_chars2_uuid, chars2_value, 1, TXRX_BUF_LEN, GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_READ);
GattCharacteristic  characteristic3(service1_chars3_uuid, chars3_value, 3, TXRX_BUF_LEN, GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_NOTIFY);
GattCharacteristic  characteristic4(service1_chars4_uuid, chars4_value, 1, TXRX_BUF_LEN, GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_READ);
GattCharacteristic  characteristic5(service1_chars5_uuid, chars5_value, 3, TXRX_BUF_LEN, GattCharacteristic::BLE_GATT_CHAR_PROPERTIES_NOTIFY);
GattCharacteristic *uartChars[] = {&characteristic1, &characteristic2, &characteristic3, &characteristic4, &characteristic5};

GattService         uartService(service1_uuid, uartChars, sizeof(uartChars) / sizeof(GattCharacteristic *));

//For Sensor Input
int   A_in_humid = A3;  // アナログ入力ピン番号
int   A_in_temp = A4;  // アナログ入力ピン番号

float A_val_humid;        // アナログ入力値(0〜203)
float A_val_temp;        // アナログ入力値(0〜203)

float tempC   = 0;  // 摂氏値( ℃ )
float humid = 0;

uint8_t             thermTempPayload[2] = { 0, 0 };

/**
 * @brief A very quick conversion between a float temperature and 11073-20601 FLOAT-Type.
 * @param temperature The temperature as a float.
 * @return The temperature in 11073-20601 FLOAT-Type format.
 */
uint32_t quick_ieee11073_from_float(float temperature)
{
    uint8_t  exponent = 0xFF; //exponent is -1
    uint32_t mantissa = (uint32_t)(temperature*10);
    
    return ( ((uint32_t)exponent) << 24) | mantissa;
}

void readSensorValues(){
 /* 
  * Humid Value range:
  * 0   ~ 300 : dry soil
  * 300 ~ 700 : humid soil
  * 700 ~ 950 : in water
  */
  //http://www.dfrobot.com/wiki/index.php?title=Moisture_Sensor_(SKU:SEN0114)
  for(int i=0;i<10;i++){
    A_val_humid = analogRead(A3);
  }
  //http://www.dfrobot.com/index.php?route=product/product&product_id=76#.Ve-zi53tmko
  for(int i=0;i<10;i++){
    A_val_temp = analogRead(A4);
  }
  
  humid = A_val_humid * (500.0 / 330.0); // for 3.3V input
  tempC = A_val_temp / 1024.0  * 330.0;  // for 3.3V input

  Serial1.print("Humid:");
  Serial1.print(humid);
  Serial1.print(":");
  Serial1.print(A_val_humid);
  
  Serial1.print(",   ");
  
  Serial1.print("Temp:");
  Serial1.print(tempC);
  Serial1.print(":");
  Serial1.print(A_val_temp);
  Serial1.println("");
}


static void disconnectionCallBack(Gap::Handle_t handle, Gap::DisconnectionReason_t reason)
{
    Serial1.println("Disconnected ");
    Serial1.println("Restart advertising ");
    ble.startAdvertising();
}

// GATT call back handle
void writtenHandle(const GattWriteCallbackParams *Handler)
{
    uint8_t buf[TXRX_BUF_LEN];
    uint16_t bytesRead, index;

    Serial1.println("Write Handle : ");
    if (Handler->handle == characteristic1.getValueAttribute().getHandle())
    {
        ble.readCharacteristicValue(characteristic1.getValueAttribute().getHandle(), buf, &bytesRead);
        for(byte index=0; index<bytesRead; index++)
        {
            Serial1.print(buf[index], HEX);
        }
        Serial1.println(" ");
    }
}

uint8_t buf[2];

// Task handle
void m_1s_handle(void)
{
    Serial1.println("1s Loop ");
    readSensorValues();

    uint16_t iHumid = round(humid);
    memcpy(buf, &iHumid, 2);    
    ble.updateCharacteristicValue(characteristic2.getValueAttribute().getHandle(), buf, 2);
    ble.updateCharacteristicValue(characteristic3.getValueAttribute().getHandle(), buf, 2);

    
    uint16_t iTemp = round(tempC);
    memcpy(buf, &iTemp, 2);
    ble.updateCharacteristicValue(characteristic4.getValueAttribute().getHandle(), buf, 2);
    ble.updateCharacteristicValue(characteristic5.getValueAttribute().getHandle(), buf, 2);
}


void ble_setup() {
  
    ticker.attach(m_1s_handle, 1);
    // put your setup code here, to run once
    pinMode(D13, OUTPUT);
    
    Serial1.println("Start BLE ");
    ble.init();
    ble.onDisconnection(disconnectionCallBack);
    ble.onDataWritten(writtenHandle);
      
    // setup adv_data and srp_data
    ble.accumulateAdvertisingPayload(GapAdvertisingData::BREDR_NOT_SUPPORTED);
    ble.accumulateAdvertisingPayload(GapAdvertisingData::SHORTENED_LOCAL_NAME,
                                     (const uint8_t *)"TXRX", sizeof("TXRX") - 1);
    ble.accumulateAdvertisingPayload(GapAdvertisingData::COMPLETE_LIST_128BIT_SERVICE_IDS,
                                     (const uint8_t *)uart_base_uuid_rev, sizeof(uart_base_uuid_rev));
                                     
    ble.accumulateScanResponse(GapAdvertisingData::SHORTENED_LOCAL_NAME, 
                                     (const uint8_t *)"hello", sizeof("hello") - 1);                        
    ble.accumulateScanResponse(GapAdvertisingData::COMPLETE_LIST_128BIT_SERVICE_IDS,
                                     (const uint8_t *)uart_base_uuid_rev, sizeof(uart_base_uuid_rev));     
                
    // set adv_type
    ble.setAdvertisingType(GapAdvertisingParams::ADV_CONNECTABLE_UNDIRECTED);    
  // add service
    ble.addService(uartService);
    // set device name
    ble.setDeviceName((const uint8_t *)"BLE Ground Sensor");
    // set tx power,valid values are -40, -20, -16, -12, -8, -4, 0, 4
    ble.setTxPower(4);

    // set adv_interval, 100ms in multiples of 0.625ms.
    ble.setAdvertisingInterval(160);
    // set adv_timeout, in seconds
    ble.setAdvertisingTimeout(0);
    // ger BLE stack version
    Serial1.println( ble.getVersion() );
    // start advertising
    ble.startAdvertising();

    Serial1.println("start advertising ");
}

// the setup function runs once when you press reset or power the board
void setup() {
  // initialize digital pin 13 as an output.
  pinMode(13, OUTPUT);
  pinMode(A_in_humid, INPUT);
  pinMode(A_in_temp, INPUT);
  
  Serial1.begin(9600);

  ble_setup();
}

// the loop function runs over and over again forever
void loop() {
  Serial1.println("=========");
  
  ble.waitForEvent();
  
  digitalWrite(13, HIGH);   // turn the LED on (HIGH is the voltage level)
  delay(100);              // wait for a second
  digitalWrite(13, LOW);    // turn the LED off by making the voltage LOW
  delay(100);              // wait for a second
}
