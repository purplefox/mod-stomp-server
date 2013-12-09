package io.vertx.stomp;

import org.vertx.java.core.buffer.Buffer;

import java.util.Map;

/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 */
public class StompMessage {

  private final Map<String, String> headers;

  private final Buffer body;


  public StompMessage(Map<String, String> headers, Buffer body) {
    this.headers = headers;
    this.body = body;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Buffer getBody() {
    return body;
  }
}
