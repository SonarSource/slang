// Databricks notebook source
// DBTITLE 1,Import required libraries
import java.sql.Timestamp

// COMMAND ----------

// DBTITLE 1,Create input parameter
dbutils.widgets.text("parameterName","","Parameter Name")
