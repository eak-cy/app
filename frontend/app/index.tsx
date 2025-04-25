import { View, Text } from "react-native";
import { Redirect } from "expo-router";

const Dashboard = () => {
    const user = undefined;

    if (!user) {
        return <Redirect href="/login" />
    }

    return <View  style={{ flex: 1 }}><Text>Welcome back farmer!</Text></View>
}

export default Dashboard;