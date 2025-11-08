package com.tejas.redis.resp;

/** Marker interface for RESP objects. */
public sealed interface RespObject permits RespSimpleString, RespError, RespInteger, RespBulkString, RespArray {}
