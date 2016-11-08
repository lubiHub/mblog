/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.message;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * 对对象的string类型属性进行标签转化
 * 
 * @author mhlx
 *
 */
public class MessageSerializer extends JsonSerializer<Message> {

	@Autowired
	private Messages messages;

	@Override
	public Class<Message> handledType() {
		return Message.class;
	}

	@Override
	public void serialize(Message value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		String message = HtmlUtils.htmlEscape(messages.getMessage(value));
		gen.writeString(message);
	}

	public MessageSerializer() {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
	}

}
