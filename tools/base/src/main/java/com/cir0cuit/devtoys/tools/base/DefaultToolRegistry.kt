package com.cir0cuit.devtoys.tools.base

import com.cir0cuit.devtoys.tools.base.Tool
import com.cir0cuit.devtoys.tools.base.ToolRegistry

class DefaultToolRegistry(private val tools: List<Tool>) : ToolRegistry {
    override fun getAllTools(): List<Tool> = tools
    override fun getToolById(id: String): Tool? = tools.find { it.descriptor.id == id }
}
