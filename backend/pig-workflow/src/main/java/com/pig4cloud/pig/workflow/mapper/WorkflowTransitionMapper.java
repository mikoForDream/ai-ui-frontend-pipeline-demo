package com.pig4cloud.pig.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pig4cloud.pig.workflow.entity.WorkflowTransition;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowTransitionMapper extends BaseMapper<WorkflowTransition> {
}
