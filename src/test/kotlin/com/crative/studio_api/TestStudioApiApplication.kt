package com.crative.studio_api

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<StudioApiApplication>().with(TestcontainersConfiguration::class).run(*args)
}
