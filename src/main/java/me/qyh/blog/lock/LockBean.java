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
package me.qyh.blog.lock;

import java.io.Serializable;

public class LockBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Lock lock;
	private String redirectUrl;
	private LockResource lockResource;

	public LockBean(Lock lock, LockResource lockResource, String redirectUrl) {
		this.lock = lock;
		this.redirectUrl = redirectUrl;
		this.lockResource = lockResource;
	}

	public Lock getLock() {
		return lock;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public LockResource getLockResource() {
		return lockResource;
	}

}
