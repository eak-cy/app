import { useState, useEffect } from "react";
import { View, Text } from "react-native";
import { Redirect } from "expo-router";
import { supabase } from "../libs/supabase"; 
import { Session } from "@supabase/supabase-js";

const Dashboard = () => {
    const [session, setSession] = useState<Session | null>(null)

    useEffect(() => {
        supabase.auth.getSession().then(({ data: { session } }) => {
          setSession(session)
        })
         supabase.auth.onAuthStateChange((_event, session) => {
          setSession(session)
        })
      }, [])


    if (!session) {
        return <Redirect href="/login" />
    }

    return <View  style={{ flex: 1 }}><Text>Welcome back farmer!</Text></View>
}

export default Dashboard;