const fs = require('fs');

const path = 'app/src/main/java/com/example/daadi/data/repository/supabase/SupabaseRepositories.kt';
let content = fs.readFileSync(path, 'utf8');

const replacement = `
    fun createAnnouncementFull(announcement: SupabaseAnnouncement) {
        if (!network.userHasPermission("manage_config")) return
        network.scope.launch {
            if (network.isConfigured) {
                val reqBody = network.moshi.adapter(SupabaseAnnouncement::class.java).toJson(announcement.copy(id = 0)) // let DB generate ID
                val request = Request.Builder()
                    .url("\${network.supabaseUrl}/rest/v1/announcements")
                    .headers(network.getHeaders())
                    .post(reqBody.toRequestBody("application/json".toMediaType()))
                    .build()
                network.client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {}
                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            network.logAdminAction("REMOTE_CONFIG_UPDATE", "announcements")
                            network.fetchAnnouncements()
                        }
                    }
                })
            } else {
                val maxId = network._announcements.value.maxOfOrNull { it.id } ?: 0
                val nextId = maxId + 1
                network._announcements.value = listOf(announcement.copy(id = nextId)) + network._announcements.value
                network.saveSimulatorAnnouncements()
            }
        }
    }
`;

if (!content.includes('fun createAnnouncementFull')) {
    content = content.replace(/fun createAnnouncement\(title: String, content: String, isActive: Boolean\) \{[\s\S]*?\}\n/, match => match + replacement);
    fs.writeFileSync(path, content, 'utf8');
    console.log('Added createAnnouncementFull.');
} else {
    console.log('Already exists.');
}
