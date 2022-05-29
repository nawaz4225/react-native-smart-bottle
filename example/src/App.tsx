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
        title="Send Command"
        onPress={() => SmartBottle.sendTyle(1)}
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
