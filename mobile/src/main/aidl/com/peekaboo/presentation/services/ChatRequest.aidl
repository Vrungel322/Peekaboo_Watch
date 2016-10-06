package com.peekaboo.presentation.services;

import com.peekaboo.presentation.services.ChatListener;

interface ChatRequest {

    // Alike a POST method of HTTP - 'action' is a path, 'params' are request params,
    // and 'data' is a json-encoded object; returns also a json-encoded object.
    String post(in String action, in Map params, in String data);

    // Set a listener to be notified about arrived messages or status change
    void listen(in ChatListener listener);
}

