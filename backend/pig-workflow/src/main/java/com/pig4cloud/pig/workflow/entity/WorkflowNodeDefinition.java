package com.pig4cloud.pig.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pig4cloud.pig.common.mybatis.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流节点定义。
 */
@Data
@TableName("wf_node_definition")
@EqualsAndHashCode(callSuper = true)
public class WorkflowNodeDefinition extends BaseEntity {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private Long definitionId;

	private String nodeKey;

	private String nodeName;

	private String nodeType;

	private Integer sortOrder;

	private Boolean startNode;

	private Boolean endNode;

	private String nextNodeKey;

	private String configJson;

	@TableLogic
	private String delFlag;

}
