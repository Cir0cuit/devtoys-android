package com.cir0cuit.devtoys.tools.base

interface ToolRegistry {
    fun getAllTools(): List<Tool>
    fun getToolById(id: String): Tool?
}
