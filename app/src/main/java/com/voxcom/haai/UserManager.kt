import android.content.Context
import com.google.gson.Gson
import com.voxcom.haai.User

object UserManager {

    private const val PREF_NAME = "user_prefs"
    private const val KEY_USER = "user_data"

    fun saveUser(context: Context, user: User) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(user)
        prefs.edit().putString(KEY_USER, json).apply()
    }

    fun getUser(context: Context): User? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_USER, null)
        return Gson().fromJson(json, User::class.java)
    }

    fun clearUser(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}