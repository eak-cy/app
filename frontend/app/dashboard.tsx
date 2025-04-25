import { Redirect } from "expo-router";
import { useEffect, useState } from "react";
import { View, Text } from "react-native";

import AsyncStorage from '@react-native-async-storage/async-storage';

const Dashboard = () => {

    const [user, setUser] = useState();

    useEffect(() => {
        console.log('getting user session from async storage')

        const getUserSession = async () => {
            try {
                const userSession = await AsyncStorage.getItem('user-session');
                if (userSession !== null) {
                  // value previously stored
                  setUser(JSON.parse(userSession));
                }
              } catch (e) {
                // error reading value
              }
        }

        getUserSession();
    }, []);


    if (!user) {
        return <Redirect href="/login" />
    }

    return <View  style={{ flex: 1 }}><Text>Welcome back {user.user.email}!</Text></View>
}

export default Dashboard;