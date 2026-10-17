package com.nya.app.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface MemoryRepository {
    val memories: Flow<List<Memory>>
}

class FakeMemoryRepository : MemoryRepository {
    override val memories: Flow<List<Memory>> = MutableStateFlow(
        listOf(
            Memory("1", "Sunset at the pier", "Karachi", "Today, 6:42 PM",
                "Golden hour by the water. You said it felt like the day finally slowed down.", Scene.Sea, 8),
            Memory("2", "Beach day", "Sea View", "Fri, 4:15 PM",
                "Windy, cold water, and the best chai afterwards.", Scene.Mount),
            Memory("3", "Late night idea", "Home", "Sun, 11:48 PM",
                "A voice note about making memories searchable by feeling.", Scene.Dusk),
            Memory("4", "Morning walk", "Park", "Mon, 7:05 AM",
                "30 minutes, no phone. You noticed the birds near the east gate.", Scene.Forest, 6),
            Memory("5", "Coffee with Ali", "Clifton", "Yesterday, 11:10 AM",
                "Talked about the app launch. Ali suggested a softer onboarding.", Scene.Yoga),
            Memory("6", "Family dinner", "Home", "Sat, 8:30 PM",
                "Everyone together. Biryani, lots of laughing.", Scene.City)
        )
    )
}
