# /add-assistant-tool

Add a new tool to the AI assistant. Updates all three required locations to keep them in sync.

## Ask me for:
1. Tool name (e.g., `generateReportForCustomer`)
2. Parameters (with types, e.g., `customerId: Long, format: String`)
3. Return type (e.g., `Document` entity or `String` or `void`)
4. Description of what the tool does
5. Optional: description of confirmation flow if the tool performs bulk operations

## What I'll do:
1. **Add `@Tool` method to `tools/CrmTools.java`**
   - Proper `@Tool` annotation with description
   - All parameters documented with `@param` Javadoc
   - Implementation calling appropriate service methods
   - Confirm-mode wrapper if bulk operations

2. **Add corresponding `case` to `service/AssistantService.handleToolCall()`**
   - Routes tool name to the new method
   - Passes parameters correctly
   - Handles response

3. **Update `service/AssistantService.buildSystemPrompt()`**
   - Add the tool to the list of available tools documented for the LLM
   - Include parameter types and usage notes
   - Note any confirmation flow

4. **Verify sync**: All three locations reference the same tool name and parameters

## Example:
If you request a tool called `generateQuarterlyReport`:

**CrmTools.java**:
```java
@Tool(description = "Generate a quarterly report for opportunities in the current tenant")
public Document generateQuarterlyReport(
    @ToolParam(description = "Report year, e.g., 2026") Integer year,
    @ToolParam(description = "Quarter 1-4") Integer quarter) {
  Long tenantId = TenantContext.getTenantId();
  if (tenantId == null) throw new ForbiddenException("Tenant context not set");
  return reportingService.generateQuarterlyReport(tenantId, year, quarter);
}
```

**AssistantService.handleToolCall()**:
```java
case "generateQuarterlyReport" -> {
  int year = ((Number) params.get("year")).intValue();
  int quarter = ((Number) params.get("quarter")).intValue();
  Document doc = crmTools.generateQuarterlyReport(year, quarter);
  return objectMapper.valueToTree(doc);
}
```

**buildSystemPrompt()**:
```
- generateQuarterlyReport(year: int, quarter: int) -> Document
  Generate Q1-Q4 revenue and pipeline reports for the current tenant.
  Example: "generateQuarterlyReport(2026, 2)" produces a Q2 2026 report.
```

## Notes:
- All three locations must have identical parameter names and types
- If parameters change, update all three locations
- Confirm-mode is used for tools that create/update/delete multiple records
