package com.skinterface.demo.android;

interface RsvpRequest {
    // Alike a POST method of HTTP - 'action' is a path, 'params' are request params,
    // and 'data' is a json-encoded object; returns also a json-encoded object.
    String post(in String action, in Map params, in String data);
}
