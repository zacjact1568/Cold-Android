package me.imzack.app.cold.event

class CityAddedEvent(eventSource: String, cityId: String, position: Int) : BaseEvent(eventSource, cityId, position)
