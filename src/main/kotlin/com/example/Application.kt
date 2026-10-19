package com.example

import com.example.models.Item
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    val items = mutableListOf(
        Item(1, "Laptop", "High-performance gaming laptop"),
        Item(2, "Smartphone", "Latest model with OLED display")
    )

    routing {
        get("/") {
            call.respondText("Ktor Server is running successfully!")
        }

        get("/items") {
            val searchQuery = call.request.queryParameters["search"]
            val result = if (!searchQuery.isNullOrBlank()) {
                items.filter { it.name.contains(searchQuery, ignoreCase = true) }
            } else {
                items
            }
            call.respond(result)
        }

        get("/items/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                return@get
            }

            val item = items.find { it.id == id }
            if (item == null) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Item with ID $id not found"))
            } else {
                call.respond(item)
            }
        }

        post("/items") {
            try {
                val newItem = call.receive<Item>()
                if (items.any { it.id == newItem.id }) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "Item with ID ${newItem.id} already exists"))
                    return@post
                }
                items.add(newItem)
                call.respond(HttpStatusCode.Created, newItem)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body: ${e.localizedMessage}"))
            }
        }

        delete("/items/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID format"))
                return@delete
            }

            val removed = items.removeIf { it.id == id }
            if (removed) {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Item with ID $id deleted successfully"))
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "Item with ID $id not found"))
            }
        }
    }
}
