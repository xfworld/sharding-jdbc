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

package org.apache.shardingsphere.infra.rule.attribute.datanode;

import org.apache.shardingsphere.infra.datanode.DataNode;
import org.apache.shardingsphere.infra.rule.attribute.RuleAttribute;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Data node rule attribute.
 */
public interface DataNodeRuleAttribute extends RuleAttribute {
    
    /**
     * Get all data nodes.
     *
     * @return all data nodes map, key is logic table name, values are data node collection belong to the key
     */
    Map<String, Collection<DataNode>> getAllDataNodes();
    
    /**
     * Get data nodes by table name.
     *
     * @param tableName table name
     * @return data nodes
     */
    Collection<DataNode> getDataNodesByTableName(String tableName);
    
    /**
     * Find first actual table name.
     *
     * @param logicTable logic table name
     * @return the first actual table name
     */
    Optional<String> findFirstActualTable(String logicTable);
    
    /**
     * Is need accumulate.
     *
     * @param tables table names
     * @return need accumulate
     */
    boolean isNeedAccumulate(Collection<String> tables);
    
    /**
     * Find logic table name via actual table name.
     *
     * @param actualTable actual table name
     * @return logic table name
     */
    Optional<String> findLogicTableByActualTable(String actualTable);
    
    /**
     * Find actual table name via catalog.
     *
     * @param catalog catalog
     * @param logicTable logic table name
     * @return actual table name
     */
    Optional<String> findActualTableByCatalog(String catalog, String logicTable);
    
    /**
     * Whether replica based distribution.
     *
     * @return is replica based distribution or not
     */
    boolean isReplicaBasedDistribution();
}
