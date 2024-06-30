#include<Arduino.h>
#include "definitions.h"

tTraining setTraining;
intensity_t previousIntensity = NOINTENSITY;
tSummary summary = { 0, 0, 0 };
bool trainingReceived;

unsigned long lctMetersCalculated;
unsigned long lastTimeCalculatedTime;

bool summarySent;

void defaultTraining() 
{
  setTraining.setTime = DEFAULT_TIME;
  setTraining.setMeters = DEFAULT_METERS;
  setTraining.dynamicMusic = DEFAULT_DYNAMIC_MUSIC;
  setTraining.enableBuzzer = DEFAULT_BUZZER;
 
  setTraining.personalizedTraining = DEFAULT_TRAINING_TYPE;
  trainingReceived = true;
  lctMetersCalculated = millis();
  lastTimeCalculatedTime = millis();
  lowSpeed = MID_INTENSITY_LOW_SPEED;
  highSpeed = MID_INTENSITY_HIGH_SPEED;
}

void resetTraining()
{
  showTrainingState("Restarting");

  previousIntensity = NOINTENSITY;
  trainingReceived = false;
  summarySent = false;

  
  lcd.setRGB(RGB_HIGH, RGB_HIGH, RGB_LOW);

  setTraining.setMeters = 0;
  setTraining.setTime = 0;
  summary.averageSpeed = 0;
  summary.metersDone = 0;
  summary.timeDone = 0;
  rang25 = false;
  rang50 = false;
  rang75 = false;
  rang100 = false;
}

void resumeTraining()
{
  showTrainingState("Resumed");
  sendTrainningState("RESUMED");
  lastTimeCalculatedTime = millis();
  lctMetersCalculated = millis();
  lcd.clear();
}

void updateTrainingState() 
{
  updateDistance();
  updateTime();
  showSpeed();
  turnOnIntensityLed();
  turnOnDynamicMusic();
  turnOnBuzzer();
}

void trainingFinished(const char *mensaje) 
{
  showTrainingState(mensaje);
  sendSummary();

  summarySent = true;
}

void startTraining() 
{
  showTrainingState("Started");
  sendTrainningState("STARTED");
  lctMetersCalculated = millis();
  lastTimeCalculatedTime = millis();
  lcd.clear();
}