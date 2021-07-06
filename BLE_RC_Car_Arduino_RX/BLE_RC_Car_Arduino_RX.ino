#include <LiquidCrystal_I2C.h>
#include <SoftwareSerial.h>
#define MAX_BTCMDLEN 128

SoftwareSerial BTSerial(10, 11); // HC-05的TXD,RXD腳位
LiquidCrystal_I2C lcd(0x27,16,2);  // 設定 LCD
byte cmd[MAX_BTCMDLEN]; // received 128 bytes from an Android system
int len = 0; // received command length
bool check=true;
int timer =0;
bool turn =false;
void setup(){
    lcd.init();            // initialize the lcd
    lcd.backlight();
    BTSerial.begin(9600);
    Serial.begin(9600);
    lcd.setCursor(0,0);
    lcd.print("BT is Ready ");
    lcd.setCursor(0,1);
    lcd.print("Check the device");
}
void loop(){
    char str[MAX_BTCMDLEN];
    int insize, ii;  
    int tick=0;
    char * pch;
    int speed = 0;
    double angle =0;
   
    while( tick<MAX_BTCMDLEN ){ // 因為包率同為9600, Android送過來的字元可能被切成數份
          //
          if((insize=(BTSerial.available()))>0){  // 讀取藍牙訊息
              turn = true;
              for( ii=0; ii<insize; ii++ )
                  cmd[(len++)%MAX_BTCMDLEN]=char(BTSerial.read());   
          }else{
                tick++;
          }
   }
       
     if(len){ // 用LCD1602顯示從Android手機傳過來的訊息
          timer=0;
          sprintf(str,"%s",cmd);
          
          if((strcmp(str,"@@")==0)){
            check = !check;
            lcd.clear();
          }else if((strncmp(str,"@@",2)==0)){
            check = !check;
            lcd.clear();
          }
          if(check){
            lcd.setCursor(0,0);
            lcd.print("Speed:             ");
            lcd.setCursor(0,1);
            lcd.print("Angle:             ");
            pch = strtok (str," ");
            if(atoi(pch)<=80){
              speed = atoi(pch);
            }
            lcd.setCursor(8,0);
            lcd.print(speed);
            pch = strtok (NULL," ");
            angle = atof(pch);
            lcd.setCursor(8,1);
            lcd.print(angle);
            delay(20);
        }else{
            pch = strtok (str,"@@");
            lcd.setCursor(0,0);
            lcd.print("Send the Msg:     ");
            lcd.setCursor(0,1);
            lcd.print("                 ");
            lcd.setCursor(0,1);
            lcd.print(pch);
            delay(20);   
        }
          memset(cmd, NULL, sizeof cmd);
          cmd[0] = '\0'; 
     }else{
           if(turn)
              timer++;
           if(timer>=9000 && turn && BTSerial.available()==0){
              check=true;
              timer =0;
              lcd.clear();
              lcd.setCursor(0,0);
              lcd.print("UnConnected,    ");
              lcd.setCursor(0,1);
              lcd.print("check the device");
           }
     }
     len = 0;
}
