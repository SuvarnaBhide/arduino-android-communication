void setup() {
  Serial1.begin(9600); //9600 bits transferred in a second
  pinMode(13, OUTPUT);
}

void loop() {

  if(Serial1.available() > 0){
    //reading data received from the bluetooth module
    char data = Serial1.read();
    switch(data){
      case '1': digitalWrite(13, HIGH); break;
      case '0': digitalWrite(13, LOW); break;
      default : break;
    }
    Serial1.println(data);
  }
  delay(50);
}
