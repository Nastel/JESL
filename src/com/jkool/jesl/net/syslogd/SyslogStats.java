/*
 * Copyright 2015 JKOOL, LLC.
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
package com.jkool.jesl.net.syslogd;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class maintains syslog target timing and performance
 * statistics.
 *
 * @see SyslogTNT4JEventHandler
 * @version $Revision: 1$
 */
class SyslogStats {
	AtomicLong nanoStamp = new AtomicLong(0);
	AtomicLong hitCount = new AtomicLong(0);
	
	SyslogStats() {
		nanoStamp.set(System.nanoTime());
	}
	
	SyslogStats(long nanotime) {
		nanoStamp.set(nanotime);
	}

	long getAgeNanos() {
		return (System.nanoTime() - nanoStamp.get());
	}
	
	long getAgeNanos(long nanos) {
		return (nanos - nanoStamp.get());
	}
	
	long getNanoTime() {
		return nanoStamp.get();
	}
	
	boolean updateNanoTime(long expected, long update) {
		return nanoStamp.compareAndSet(expected, update);		
	}
	
	long hit() {
		return hitCount.addAndGet(+1);
	}
	
	long hit(long count) {
		return hitCount.addAndGet(count);
	}
	
	long getHits() {
		return hitCount.get();
	}
}