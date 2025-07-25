/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.db.protocol.firebird.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.infra.exception.generic.UnsupportedSQLOperationException;

/**
 * Firebird value format.
 */
@RequiredArgsConstructor
@Getter
public enum FirebirdValueFormat {
    
    TEXT(0),
    
    BINARY(1),
    
    BOOLEAN(2),
    
    INT(3),
    
    STRING(4);
    
    private final int code;
    
    /**
     * Value of.
     *
     * @param code format code
     * @return Firebird value format
     * @throws UnsupportedSQLOperationException unsupported SQL operation exception
     */
    public static FirebirdValueFormat valueOf(final int code) {
        switch (code) {
            case 0:
                return TEXT;
            case 1:
                return BINARY;
            default:
                throw new UnsupportedSQLOperationException(String.format("Unsupported Firebird format code `%s`", code));
        }
    }
}
