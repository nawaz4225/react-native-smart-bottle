import * as React from 'react';

import { StyleSheet, View, Text, Button } from 'react-native';
import { SmartBottle, eventEmitter } from 'react-native-smart-bottle';

export default function App() {
  const [result, setResult] = React.useState<number | undefined>();

  React.useEffect(() => {
    
  }, []);

  eventEmitter.addListener('Event', (event) => {
    console.log(event.value) // "someValue"
    setResult(event.value);
 });

  return (
    <View style={styles.container}>

      <Text style={styles.baseText}>
        Result: {result}
        </Text>

      <Button
        title="Request Permissions"
        onPress={() => SmartBottle.requestRequiredPermissions()}
      />

      <Button
        title="Initialize"
        onPress={() => SmartBottle.initializeBLE()}
      />
      <Button
        title="Scan"
        onPress={() => SmartBottle.Scanbluetooth()}
      />  

      <Button
        title="Handshake Command"
        onPress={() => SmartBottle.handShake()}
      />  

      <Button
        title="Set Time Command"
        onPress={() => SmartBottle.setTime("2022", 2, 2, 12, 45, 1)}
      />  

      <Button
        title="Get Time Command"
        onPress={() => SmartBottle.getTime()}
      />  

      <Button
        title="Get Battery Command"
        onPress={() => SmartBottle.getBattery()}
      /> 

      <Button
        title="Set Water Intake Goal Command"
        onPress={() => SmartBottle.setIntakeGoal(2021)}
      /> 

      <Button
        title="Get Water Intake Goal Command"
        onPress={() => SmartBottle.getIntakeGoal()}
      />  

      <Button
        title="Get current Water Intake Command"
        onPress={() => SmartBottle.getCurrentIntake()}
      />  

      <Button
        title="Get water directory Command"
        onPress={() => SmartBottle.getWaterDirectory("2022", 5, 31)}
      />  

      <Button
        title="Delete Day Data Command"
        onPress={() => SmartBottle.deleteWaterDirectory("2022", 5, 31)}
      />  

      <Button
        title="Set User Information Command"
        onPress={() => SmartBottle.setUserInformation("Shahbaz", "Male", 30)}
      />  

      <Button
        title="Get User Information Command"
        onPress={() => SmartBottle.getUserInformation()}
      />  
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  baseText: {
    fontFamily: "Cochin"
  },
  titleText: {
    fontSize: 20,
    fontWeight: "bold"
  },
});
