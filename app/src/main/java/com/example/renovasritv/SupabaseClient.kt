package com.example.renovasritv

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.serialization.json.Json

object SupabaseConfig {
    const val SUPABASE_URL = "https://xbeslcqosyhyuyxztpov.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InhiZXNsY3Fvc3loeXV5eHp0cG92Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzU2MzgwNTAsImV4cCI6MjA5MTIxNDA1MH0.gmfnwErjtQwUNgmPNKrKmEYY16LaQcnkrbqLe205f_A"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        httpEngine = OkHttp.create()
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
        install(Postgrest)
        install(Auth)
        install(Realtime)
        install(Storage)
    }
}
