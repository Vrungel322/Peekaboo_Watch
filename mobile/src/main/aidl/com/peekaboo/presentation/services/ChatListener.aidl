package com.peekaboo.presentation.services;

oneway interface ChatListener {
    // 'action' is a message event type (received, status changed, etc)
    // 'message' is a json-encoded object for the message
    void onMessage(String action, String message);
}
