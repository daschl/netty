/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import java.util.concurrent.locks.LockSupport;

/**
 * Custom {@link BackoffStrategy} which uses {@link LockSupport#parkNanos(long)} to delay
 * selector blocking while keeping the event loop responsive.
 *
 * This strategy works very well if one needs to write from outside into the event loop to avoid
 * selector wakeup overhead.
 */
public final class ParkingBackoffStrategy implements BackoffStrategy {

    private int counter;

    @Override
    public boolean delaySelect(int selectNowCount, boolean hasTasks) {
        if (selectNowCount != 0 || hasTasks) {
            return false;
        }
        counter++;

        if (counter < 1000) {
            LockSupport.parkNanos(1);
        } else if (counter < 3000) {
            LockSupport.parkNanos(1000);
        } else if (counter > 1000000) {
            counter = 0;
            return false;
        }

        return true;
    }

}
