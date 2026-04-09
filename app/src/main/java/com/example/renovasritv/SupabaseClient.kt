package com.example.renovasritv

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseConfig {
    // TODO: Replace with your actual project credentials from Supabase Dashboard
    const val SUPABASE_URL = "https://your-project-id.supabase.co"
    const val SUPABASE_ANON_KEY = "your-anon-key"

    val client = createSupabaseClient(
        supabaseUrl = "https://xbeslcqosyhyuyxztpov.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhiZXNsY3Fvc3loeXV5eHp0cG92Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU2MzgwNTAsImV4cCI6MjA5MTIxNDA1MH0.gmfnwErjtQwUNgmPNKrKmEYY16LaQcnkrbqLe205f_A"
    ) {
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
