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
package me.qyh.blog.web.controller.form;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class BlogFileUpload {

	private List<MultipartFile> files;
	private Integer parent;
	private Integer server;

	public List<MultipartFile> getFiles() {
		return files;
	}

	public void setFiles(List<MultipartFile> files) {
		this.files = files;
	}

	public Integer getParent() {
		return parent;
	}

	public void setParent(Integer parent) {
		this.parent = parent;
	}

	public Integer getServer() {
		return server;
	}

	public void setServer(Integer server) {
		this.server = server;
	}

	@Override
	public String toString() {
		return "BlogFileUpload [files=" + files + ", parent=" + parent + ", server=" + server + "]";
	}

}
