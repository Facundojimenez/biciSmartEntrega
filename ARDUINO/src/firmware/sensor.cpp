#include <Arduino.h>
#include "definitions.h"

// External Variables / Calculo de velocidad
unsigned long lastPedalActivationTime = 0;
unsigned long currentPedalActivationTime = 0;
unsigned long currentTime = 0;
unsigned long timeDiff = 0;
bool pedalWasPressed = false;
float lowSpeed;
float highSpeed;

void checkSpeedSensor()
{

  currentTime = millis();
  // check if bike is stopped
  if (currentTime - lastPedalActivationTime > BIKE_IS_STOPPED_TIMEOUT)
  {
    speed_MS = 0;
  }

  int sensorValue = analogRead(HALL_SENSOR_PIN);
  Serial.println(sensorValue);

  if (!pedalWasPressed && sensorValue < LOWER_HALL_SENSOR_THRESHOLD)
  {
    currentPedalActivationTime = millis();

    if (lastPedalActivationTime > 0)
    {
      timeDiff = currentPedalActivationTime - lastPedalActivationTime;
      speed_MS = BIKE_WHEEL_CIRCUNFERENCE_PERIMETER_MM / (float)timeDiff;
    }
    pedalWasPressed = true;
    lastPedalActivationTime = currentPedalActivationTime;
  }
  else if (sensorValue > LOWER_HALL_SENSOR_THRESHOLD)
  {
    pedalWasPressed = false;
  }

  Serial.println(speed_MS);
}

void checkMediaButtonSensor()
{
  int buttonState = digitalRead(MEDIA_MOVEMENT_SENSOR_PIN);
  if (buttonState == HIGH)
  {
    currentEvent = EVENT_NEXT_MEDIA_BUTTON;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}

void checkPlayStopButtonSensor()
{
  int buttonState = digitalRead(PLAY_STOP_MEDIA_SENSOR_PIN);
  if (buttonState == HIGH)
  {
    currentEvent = EVENT_PLAY_STOP_MEDIA_BUTTON;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}

void checkCancelButtonSensor()
{
  int buttonState = digitalRead(TRAINING_CANCEL_PIN);
  if (buttonState == HIGH)
  {
    currentEvent = EVENT_TRAINING_CANCELLED;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}

void checkTrainingButtonSensor()
{
  int buttonState = digitalRead(TRAINING_CONTROL_PIN);

  if (buttonState == HIGH)
  {
    currentEvent = EVENT_TRAINING_BUTTON;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}

void checkTrainingBluetoothInterface()
{
  if (!trainingReceived)
  {
    if (BT.available() > 0)
    {
      String consoleCommand = BT.readString();
      sscanf(consoleCommand.c_str(), "%d %d %d %d %d", &(setTraining.setTime), &(setTraining.setMeters),
             &(setTraining.dynamicMusic), &(setTraining.enableBuzzer),
             &(setTraining.intensity));
      setTraining.personalizedTraining = true;

      switch (setTraining.intensity)
      {
      case LOW_INTENSITY:
        lowSpeed = LOW_LEVEL_LOW_SPEED;
        highSpeed = LOW_LEVEL_HIGH_SPEED;
        break;
      case MID_INTENSITY:
        lowSpeed = MID_INTENSITY_LOW_SPEED;
        highSpeed = MID_INTENSITY_HIGH_SPEED;
        break;
      case HIGH_INTENSITY:
        lowSpeed = HIGH_INTENSITY_LOW_SPEED;
        highSpeed = HIGH_INTENSITY_HIGH_SPEED;
        break;
      default:
        break;
      }

      currentEvent = EVENT_TRAINING_RECEIVED;
      trainingReceived = true;
    }
  }
  else
  {
    if (BT.available() > 0)
    {
      String consoleCommand = BT.readString();
      if (consoleCommand == "RESUME" || consoleCommand == "PAUSE")
        currentEvent = EVENT_TRAINING_BUTTON;
      if (consoleCommand == "CANCEL")
        currentEvent = EVENT_TRAINING_CANCELLED;
    }
    else
    {
      currentEvent = EVENT_CONTINUE;
    }
  }
}

void checkSummaryBluetooth()
{
  if (summarySent)
  {

    currentEvent = EVENT_TRAINING_RESTARTED;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}

void checkProgress()
{
  if (summary.timeDone == 0)
  {
    currentEvent = EVENT_CONTINUE;
    return;
  }

  if (setTraining.setTime != 0)
  {
    if (summary.timeDone >= (setTraining.setTime))
    {
      currentEvent = EVENT_TRAINING_CONCLUDED;
    }
  }
  else
  {
    if (summary.metersDone >= setTraining.setMeters)
    {
      currentEvent = EVENT_TRAINING_CONCLUDED;
    }
  }
}

void checkVolumeSensor()
{
  if (summary.timeDone == 0)
  {
    currentEvent = EVENT_CONTINUE;
    return;
  }

  int value = analogRead(VOLUME_SENSOR_PIN);
  int currentVolumeValue = map(value, MIN_POT_VALUE, MAX_POT_VALUE, MIN_VOLUME, MAX_VOLUME);

  if (currentVolumeValue != lastVolumeValue)
  {
    currentEvent = EVENT_VOLUME_CHANGE;
    lastVolumeValue = currentVolumeValue;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}

void (*check_sensor[NUMBER_OF_SENSORS])() =
    {
        checkSpeedSensor,
        checkCancelButtonSensor,
        checkTrainingButtonSensor,
        checkPlayStopButtonSensor,
        checkMediaButtonSensor,
        checkTrainingBluetoothInterface,
        checkSummaryBluetooth,
        checkProgress,
        checkVolumeSensor};

void get_event()
{
  unsigned long currentTime = millis();
  if ((currentTime - previousTime) > MAX_SENSOR_LOOP_TIME)
  {
    check_sensor[index]();
    index = ++index % NUMBER_OF_SENSORS;
    previousTime = currentTime;
  }
  else
  {
    currentEvent = EVENT_CONTINUE;
  }
}
