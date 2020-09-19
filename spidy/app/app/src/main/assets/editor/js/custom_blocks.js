// Variable

Blockly.Blocks['set_variable'] = {
    init: function() {
        this.appendValueInput("HOLDER")
            .setCheck(null)
            .appendField("set")
            .appendField(new Blockly.FieldVariable("var" + _spidy.variable_counter), "NAME")
            .appendField("to");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(330);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['set_variable'] = function(block) {
    var variable_name = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('NAME'), Blockly.Variables.NAME_TYPE);
    var variable_value = Blockly.JavaScript.valueToCode(block, 'HOLDER', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "set_variable", "name": "' + variable_name + '", "input": ' + variable_value + '},';
    return code;
};


Blockly.Blocks['get_variable'] = {
    init: function() {
        this.appendDummyInput()
            .appendField(new Blockly.FieldVariable("var1"), "NAME");
        this.setOutput(true, null);
        this.setColour(330);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_variable'] = function(block) {
    var variable_value = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('NAME'), Blockly.Variables.NAME_TYPE);
    var code = '{"cmd": "get_variable", "name": "' + variable_value + '"}';
    // var code = "GET_VARIABLE:" + variable_value;
    return [code, Blockly.JavaScript.ORDER_NONE];
};



// Events


Blockly.Blocks['event_click'] = {
    init: function() {
        this.appendValueInput("VAR_NAME")
            .setCheck(null)
            .appendField("click on");
        this.setInputsInline(false);
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
  };

Blockly.JavaScript['event_click'] = function(block) {
    var element = Blockly.JavaScript.valueToCode(block, 'VAR_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "event_click", "input": ' + element + '},';
    return code;
};


Blockly.Blocks['event_wait_and_click'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("wait and click on");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['event_wait_and_click'] = function(block) {
    var element = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "event_wait_and_click", "input": ' + element + '},';
    return code;
};


Blockly.Blocks['type'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("type");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['type'] = function(block) {
    var value = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "type", "input": ' + value + '},';
    return code;
};


Blockly.Blocks['press_key'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("press key")
            .appendField(new Blockly.FieldDropdown([["enter","ENTER"], ["shift","SHIFT"], ["ctrl","CTRL"], ["caps lock","CAPS_LOCK"], ["tab","TAB"], ["esc","ESC"]]), "NAME");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['press_key'] = function(block) {
    var key_name = block.getFieldValue('NAME');
    var code = '{"cmd": "press_key", "name": "' + key_name + '"},';
    return code;
};

Blockly.Blocks['wait_for'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("wait for")
            .appendField(new Blockly.FieldNumber(1, 1), "SEC")
            .appendField("seconds");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['wait_for'] = function(block) {
    var number_sec = block.getFieldValue('SEC');
    var code = '{"cmd": "wait_for", "secs": ' + number_sec + '},';
    return code;
};

Blockly.Blocks['wait_for_element'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("wait for element(s)");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['wait_for_element'] = function(block) {
    var element = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "wait_for_element", "input": ' + element + '},';
    return code;
};


Blockly.Blocks['scroll_bottom'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("scroll to bottom");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['scroll_bottom'] = function(block) {
    var code = '{"cmd": "scroll_bottom"},';
    return code;
};

Blockly.Blocks['scroll_top'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("scroll to top");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(20);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['scroll_top'] = function(block) {
    var code = '{"cmd": "scroll_top"},';
    return code;
};


// HTML

Blockly.Blocks['get_page_source'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("get page source as text");
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['get_page_source'] = function(block) {
    var code = '{"cmd": "get_page_source"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};

Blockly.Blocks['get_html_elements'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("get html element(s)");
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['get_html_elements'] = function(block) {
    var code = '{"cmd": "get_html_elements", "node": ' + _spidy.stack[block.id] + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_attribute'] = {
    init: function() {
        this.appendValueInput("VAR_NAME")
            .setCheck(null)
            .appendField("get attribute")
            .appendField(new Blockly.FieldTextInput("href"), "ATTR_NAME")
            .appendField("of");
        this.setInputsInline(false);
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['get_attribute'] = function(block) {
    var element = Blockly.JavaScript.valueToCode(block, 'VAR_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var attr_name = block.getFieldValue('ATTR_NAME');
    var code = '{"cmd": "get_attribute", "element": ' + element + ', "attr_name": "' + attr_name + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_attributes'] = {
    init: function() {
        this.appendValueInput("VAR_NAME")
            .setCheck(null)
            .appendField("get attribute")
            .appendField(new Blockly.FieldTextInput("href"), "ATTR_NAME")
            .appendField("from all");
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_attributes'] = function(block) {
    var elements = Blockly.JavaScript.valueToCode(block, 'VAR_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var attr_name = block.getFieldValue('ATTR_NAME');
    var code = '{"cmd": "get_attributes", "element": ' + elements + ', "attr_name": "' + attr_name + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_text_content'] = {
    init: function() {
        this.appendValueInput("VAR_NAME")
            .setCheck(null)
            .appendField("get text of");
        this.setInputsInline(false);
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_text_content'] = function(block) {
    var element = Blockly.JavaScript.valueToCode(block, 'VAR_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    if (!element.startsWith('(')) {
        element = '"' + element + '"';
    }
    var code = '{"cmd": "get_text_content", "element": ' + element + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_tag_name'] = {
    init: function() {
        this.appendValueInput("TEXT_NAME")
            .setCheck(null)
            .appendField("get tag name of");
        this.setInputsInline(false);
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_tag_name'] = function(block) {
    var element = Blockly.JavaScript.valueToCode(block, 'TEXT_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    if (!element.startsWith('(')) {
        element = '"' + element + '"';
    }
    var code = '{"cmd": "get_tag_name", "element": ' + element + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};

Blockly.Blocks['get_html_element_by_selector'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("get elements by selector")
            .appendField(new Blockly.FieldTextInput("a[href]"), "SELECTOR");
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_html_element_by_selector'] = function(block) {
    var text_selector = block.getFieldValue('SELECTOR');
    var code = '{"cmd": "get_html_element_by_selector", "node": {"selector": "body ' + text_selector.replace('"', '\\"') + '", "isAll": false, "elIndex": 0}}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['is_element_exist'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("is html element(s)");
        this.appendDummyInput()
            .appendField("exist");
        this.setOutput(true, null);
        this.setColour(65);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['is_element_exist'] = function(block) {
    var input = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "is_element_exist", "input": ' + input + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};



// Controls


Blockly.Blocks['for_loop'] = {
    init: function() {
        this.appendValueInput("INPUT_NAME")
            .setCheck(null)
            .appendField("for each")
            .appendField(new Blockly.FieldVariable("item" + (_spidy.variable_counter)), "NAME")
            .appendField("in");
        this.appendStatementInput("STATEMENT_NAME")
            .setCheck(null);
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['for_loop'] = function(block) {
    var loop_index_variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('NAME'), Blockly.Variables.NAME_TYPE);
    var input_list = Blockly.JavaScript.valueToCode(block, 'INPUT_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var statements = Blockly.JavaScript.statementToCode(block, 'STATEMENT_NAME');
    statements = statements.substring(0, statements.length - 1);
    var code = '{"cmd": "for_loop", "index_variable_name": "' + loop_index_variable + '", "input": ' + input_list + ', "statements": [' + statements + ']},';
    // var code = 'LOOP ' + loop_index_variable + " IN " + input_list + "\n" + code_block + "\n" + "ENDLOOP" + "\n";
    return code;
};

Blockly.Blocks['if'] = {
    init: function() {
        this.appendValueInput("CONDITION_NAME")
            .setCheck(null)
            .setAlign(Blockly.ALIGN_RIGHT)
            .appendField("if");
        this.appendStatementInput("STATEMENTS_NAME")
            .setCheck(null)
            .appendField("do");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['if'] = function(block) {
    var conditions = Blockly.JavaScript.valueToCode(block, 'CONDITION_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var statements = Blockly.JavaScript.statementToCode(block, 'STATEMENTS_NAME');
    // TODO: Assemble JavaScript into code variable.
    var code = '{"cmd": "if", "condition": ' + conditions + ', "statements": [' + statements + ']},';
    // var code = 'IF ' + conditions + "\n" + statements + "\n" + "ENDIF" + "\n";
    return code;
};



Blockly.Blocks['get_condition'] = {
    init: function() {
        this.appendValueInput("FIRST_CONDITION")
            .setCheck(null);
        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown([["=","EQUAL"], ["≠","NOT_EQUAL"], ["‏<","LESS_THAN"], ["‏≤","LESS_THAN_OR_EQUAL"], ["‏>","GTREATER_THAN"], ["‏≥","GTREATER_THAN_OR_EQUAL"]]), "NAME");
        this.appendValueInput("SECOND_CONDITION")
            .setCheck(null);
        this.setOutput(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_condition'] = function(block) {
    var value_first_condition = Blockly.JavaScript.valueToCode(block, 'FIRST_CONDITION', Blockly.JavaScript.ORDER_ATOMIC);
    var dropdown_name = block.getFieldValue('NAME');
    var value_second_condition = Blockly.JavaScript.valueToCode(block, 'SECOND_CONDITION', Blockly.JavaScript.ORDER_ATOMIC);    
    var code = '{"cmd": "get_condition", "first_condition": ' + value_first_condition + ', "operator": "' + dropdown_name + '", "second_condition": ' + value_second_condition + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_boolean'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("get boolean")
            .appendField(new Blockly.FieldDropdown([["true","TRUE"], ["false","FALSE"]]), "NAME");
        this.setOutput(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['get_boolean'] = function(block) {
    var dropdown_name = block.getFieldValue('NAME');
    var code = '{"cmd": "get_boolean", "input": "' + dropdown_name.toLowerCase() + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};

Blockly.Blocks['function'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("function")
            .appendField(new Blockly.FieldVariable("func" + _spidy.variable_counter), "VAR");
        this.appendStatementInput("FUNC")
            .setCheck(null);
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['function'] = function(block) {
    var variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('VAR'), Blockly.Variables.NAME_TYPE);
    var statements = Blockly.JavaScript.statementToCode(block, 'FUNC');
    var code = '{"cmd": "function", "name": "' + variable + '", "statements": [' + statements + ']},';
    return code;
};


Blockly.Blocks['call_function'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("call function")
            .appendField(new Blockly.FieldVariable("func1"), "NAME");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['call_function'] = function(block) {
    var variable_name = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('NAME'), Blockly.Variables.NAME_TYPE);
    var code = '{"cmd": "call_function", "name": "' + variable_name + '"},';
    return code;
};


Blockly.Blocks['break_or_continue'] = {
    init: function() {
        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown([["break out","BREAK_OUT"], ["continue with next iteration","CONTINUE"]]), "NAME")
            .appendField("of loop");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['break_or_continue'] = function(block) {
    var action = block.getFieldValue('NAME');
    var code = '{"cmd": "break_or_continue", "action": "' + action + '"},';
    return code;
};

Blockly.Blocks['for_loop_with_number'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("count with")
            .appendField(new Blockly.FieldVariable("num1"), "VAR")
            .appendField("from")
            .appendField(new Blockly.FieldNumber(1), "FROM")
            .appendField("to")
            .appendField(new Blockly.FieldNumber(10), "TO")
            .appendField("by")
            .appendField(new Blockly.FieldNumber(1, 1), "BY");
        this.appendStatementInput("NAME")
            .setCheck(null);
        this.setInputsInline(true);
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(290);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['for_loop_with_number'] = function(block) {
    var index_variable_name = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('VAR'), Blockly.Variables.NAME_TYPE);
    var number_from = block.getFieldValue('FROM');
    var number_to = block.getFieldValue('TO');
    var number_by = block.getFieldValue('BY');
    var statements = Blockly.JavaScript.statementToCode(block, 'NAME');
    var code = '{"cmd": "for_loop_with_number", "index_variable_name": "' + index_variable_name + '", "from": "' + number_from + '", "to": "' + number_to + '", "by": "' + number_by + '", "statements": [' + statements + ']},';
    return code;
};


// Navigator

Blockly.Blocks['get_input'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("ask")
            .appendField(new Blockly.FieldTextInput("enter a url"), "ATTR_NAME");
        this.setInputsInline(true);
        this.setOutput(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_input'] = function(block) {
    var prompt_string = block.getFieldValue('ATTR_NAME');
    var code = '{"cmd": "get_input", "title": "' + prompt_string + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['got_to_website'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("go to website");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['got_to_website'] = function(block) {
    var value = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "go_to_website", "input": ' + value + '},';
    return code;
};


Blockly.Blocks['download_from_url'] = {
    init: function() {
        this.appendValueInput("URL")
            .setCheck(null)
            .appendField("download from url");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['download_from_url'] = function(block) {
    var value_url = Blockly.JavaScript.valueToCode(block, 'URL', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "download_from_url", "url": ' + value_url + '},';
    return code;
};


Blockly.Blocks['debug'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("debug");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['debug'] = function(block) {
    var input = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "debug", "input": ' + input + '},';
    return code;
};


Blockly.Blocks['open_a_new_tab'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("open a new tab");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['open_a_new_tab'] = function(block) {
    var code = '{"cmd": "open_a_new_tab"},';
    return code;
};


Blockly.Blocks['switch_tab'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("switch to tab at index")
            .appendField(new Blockly.FieldNumber(0, 0), "INDEX");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['switch_tab'] = function(block) {
    var index = block.getFieldValue('INDEX');
    var code = '{"cmd": "switch_tab", "index": ' + index + '},';
    return code;
};


Blockly.Blocks['close_tab'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("close tab at index")
            .appendField(new Blockly.FieldNumber(0, 0), "INDEX");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['close_tab'] = function(block) {
    var index = block.getFieldValue('INDEX');
    var code = '{"cmd": "close_tab", "index": ' + index + '},';
    return code;
};

Blockly.Blocks['write_to_a_file'] = {
    init: function() {
        this.appendValueInput("FILE_NAME")
            .setCheck(null)
            .appendField("to file");
        this.appendValueInput("VALUE_NAME")
            .setCheck(null)
            .appendField("write");
        this.setInputsInline(true);
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(160);
        this.setTooltip("");
        this.setHelpUrl("");

        var shadowBlock = this.workspace.newBlock('get_text');
        shadowBlock.setShadow(true);
        shadowBlock.setFieldValue("out.txt", "TEXT_NAME");
        shadowBlock.initSvg();
        shadowBlock.render();
        var ob = shadowBlock.outputConnection;
        // create AT input block
        // this.appendValueInput('NAME');
        // connect shadow block
        var cc = this.getInput('FILE_NAME').connection;
        cc.connect(ob);
    }
};

Blockly.JavaScript['write_to_a_file'] = function(block) {
    var fileName = Blockly.JavaScript.valueToCode(block, 'FILE_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var input = Blockly.JavaScript.valueToCode(block, 'VALUE_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "write_to_a_file", "fileName": ' + fileName + ', "input": ' + input + '},';
    return code;
};




// Text

Blockly.Blocks['regex'] = {
    init: function() {
        this.appendValueInput("INPUT")
            .setCheck(null)
            .appendField("in text");
        this.appendDummyInput()
            .appendField("find all using regex")
            .appendField(new Blockly.FieldTextInput("[\\w]+"), "NAME");
        this.setInputsInline(true);
        this.setOutput(true, null);
        this.setColour(210);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['regex'] = function(block) {
    var text = Blockly.JavaScript.valueToCode(block, 'INPUT', Blockly.JavaScript.ORDER_ATOMIC);
    var regex = block.getFieldValue('NAME');
    var code = '{"cmd": "regex", "text": ' + text + ', "regex": "' + btoa(regex) + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};

Blockly.Blocks['get_text'] = {
    init: function() {
        this.appendDummyInput()
            .appendField("\"")
            .appendField(new Blockly.FieldTextInput(""), "TEXT_NAME")
            .appendField("\"");
        this.setOutput(true, null);
        this.setColour(210);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['get_text'] = function(block) {
    var text = block.getFieldValue('TEXT_NAME');
    var code = '{"cmd": "get_text", "text": "' + text + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['trim_space'] = {
    init: function() {
        this.appendValueInput("INPUT_NAME")
            .setCheck(null)
            .appendField("trim spaces of");
        this.setInputsInline(false);
        this.setOutput(true, null);
        this.setColour(210);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['trim_space'] = function(block) {
    var text = Blockly.JavaScript.valueToCode(block, 'INPUT_NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "trim_space", "text": ' + text + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_join_text'] = {
    init: function() {
        this.appendValueInput("TEXT1")
            .setCheck(null)
            .appendField("join text");
        this.appendValueInput("TEXT2")
            .setCheck(null)
            .appendField("with text");
        this.setInputsInline(true);
        this.setOutput(true, null);
        this.setColour(210);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};

Blockly.JavaScript['get_join_text'] = function(block) {
    var value_text1 = Blockly.JavaScript.valueToCode(block, 'TEXT1', Blockly.JavaScript.ORDER_ATOMIC);
    var value_text2 = Blockly.JavaScript.valueToCode(block, 'TEXT2', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "get_join_text", "text1": ' + value_text1 + ', "text2": ' + value_text2 + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['append_text_to_var'] = {
    init: function() {
        this.appendValueInput("NAME")
            .setCheck(null)
            .appendField("to")
            .appendField(new Blockly.FieldVariable("var1"), "NAME")
            .appendField("append text");
        this.setPreviousStatement(true, null);
        this.setNextStatement(true, null);
        this.setColour(210);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['append_text_to_var'] = function(block) {
    var variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('NAME'), Blockly.Variables.NAME_TYPE);
    var input = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "append_text_to_var", "variable": "' + variable + '", "input": ' + input + '},';
    return code;
};


Blockly.Blocks['text_contains'] = {
    init: function() {
        this.appendValueInput("TEXT")
            .setCheck(null)
            .appendField("in text");
        this.appendValueInput("VALUE")
            .setCheck(null)
            .appendField("contains text");
        this.setInputsInline(true);
        this.setOutput(true, null);
        this.setColour(210);
        this.setTooltip("");
        this.setHelpUrl("");

        var shadowBlock = this.workspace.newBlock('get_text');
        shadowBlock.setShadow(true);
        shadowBlock.setFieldValue("apple", "TEXT_NAME");
        shadowBlock.initSvg();
        shadowBlock.render();
        var ob = shadowBlock.outputConnection;
        // create AT input block
        // this.appendValueInput('NAME');
        // connect shadow block
        var cc = this.getInput('TEXT').connection;
        cc.connect(ob);

        var shadowBlock2 = this.workspace.newBlock('get_text');
        shadowBlock2.setShadow(true);
        shadowBlock2.setFieldValue("ppl", "TEXT_NAME");
        shadowBlock2.initSvg();
        shadowBlock2.render();
        var ob = shadowBlock2.outputConnection;
        var cc = this.getInput('VALUE').connection;
        cc.connect(ob);
    }
};

Blockly.JavaScript['text_contains'] = function(block) {
    var text = Blockly.JavaScript.valueToCode(block, 'TEXT', Blockly.JavaScript.ORDER_ATOMIC);
    var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "text_contains", "text": ' + text + ', "value": ' + value + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


// Math

Blockly.Blocks['get_number'] = {
    init: function() {
        this.appendDummyInput()
            .appendField(new Blockly.FieldNumber(0), "VALUE");
        this.setOutput(true, null);
        this.setColour(230);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_number'] = function(block) {
    var number_value = block.getFieldValue('VALUE');
    var code = '{"cmd": "get_number", "input": "' + number_value + '"}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};


Blockly.Blocks['get_calculation'] = {
    init: function() {
        this.appendValueInput("FIRST")
            .setCheck(null);
        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown([["+","PLUS"], ["-","MINUS"], ["×","MULTIPLY"], ["÷","DIVISION"]]), "OPERATOR");
        this.appendValueInput("SECOND")
            .setCheck(null);
        this.setInputsInline(true);
        this.setOutput(true, null);
        this.setColour(230);
        this.setTooltip("");
        this.setHelpUrl("");
    }
};


Blockly.JavaScript['get_calculation'] = function(block) {
    var value_first = Blockly.JavaScript.valueToCode(block, 'FIRST', Blockly.JavaScript.ORDER_ATOMIC);
    var operator = block.getFieldValue('OPERATOR');
    var value_second = Blockly.JavaScript.valueToCode(block, 'SECOND', Blockly.JavaScript.ORDER_ATOMIC);
    var code = '{"cmd": "get_calculation", "first": ' + value_first + ', "operator": "' + operator + '", "second": ' + value_second + '}';
    return [code, Blockly.JavaScript.ORDER_NONE];
};



// JSON

// Blockly.Blocks['create_json_object'] = {
//     init: function() {
//         this.appendDummyInput()
//             .appendField("create json object")
//             .appendField(new Blockly.FieldVariable("json"  + _spidy.variable_counter), "VAR_NAME");
//         this.setInputsInline(true);
//         this.setPreviousStatement(true, null);
//         this.setNextStatement(true, null);
//         this.setColour(120);
//         this.setTooltip("");
//         this.setHelpUrl("");
//     }
// };


// Blockly.JavaScript['create_json_object'] = function(block) {
//     var variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('VAR_NAME'), Blockly.Variables.NAME_TYPE);
//     var code = '{"cmd": "create_json_object", "variable": "' + variable + '"},';
//     return code;
// };


// Blockly.Blocks['create_json_array'] = {
//     init: function() {
//         this.appendDummyInput()
//             .appendField("create json array")
//             .appendField(new Blockly.FieldVariable("json" + _spidy.variable_counter), "VAR_NAME");
//         this.setInputsInline(true);
//         this.setPreviousStatement(true, null);
//         this.setNextStatement(true, null);
//         this.setColour(120);
//         this.setTooltip("");
//         this.setHelpUrl("");
//     }
// };

// Blockly.JavaScript['create_json_array'] = function(block) {
//     var variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('VAR_NAME'), Blockly.Variables.NAME_TYPE);
//     var code = '{"cmd": "create_json_array", "variable": "' + variable + '"},';
//     return code;
// };


// Blockly.Blocks['insert_to_json_array'] = {
//     init: function() {
//         this.appendValueInput("NAME")
//             .setCheck(null)
//             .appendField("json array")
//             .appendField(new Blockly.FieldVariable("json1"), "VAR_NAME")
//             .appendField("insert value");
//         this.setPreviousStatement(true, null);
//         this.setNextStatement(true, null);
//         this.setColour(120);
//         this.setTooltip("");
//         this.setHelpUrl("");
//     }
// };

// Blockly.JavaScript['insert_to_json_array'] = function(block) {
//     var variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('VAR_NAME'), Blockly.Variables.NAME_TYPE);
//     var value = Blockly.JavaScript.valueToCode(block, 'NAME', Blockly.JavaScript.ORDER_ATOMIC);
//     var code = '{"cmd": "insert_to_json_array", "variable": "' + variable + '", "value": ' + value + '},';
//     return code;
// };


// Blockly.Blocks['insert_to_json_object'] = {
//     init: function() {
//         this.appendValueInput("KEY")
//             .setCheck(null)
//             .appendField("json object")
//             .appendField(new Blockly.FieldVariable("json1"), "VAR_NAME")
//             .appendField("key");
//         this.appendValueInput("VALUE")
//             .setCheck(null)
//             .appendField("insert value");
//         this.setInputsInline(true);
//         this.setPreviousStatement(true, null);
//         this.setNextStatement(true, null);
//         this.setColour(120);
//         this.setTooltip("");
//         this.setHelpUrl("");

//         var shadowBlock = this.workspace.newBlock('get_text');
//         shadowBlock.setShadow(true);
//         shadowBlock.setFieldValue("key", "TEXT_NAME");
//         shadowBlock.initSvg();
//         shadowBlock.render();
//         var ob = shadowBlock.outputConnection;
//         // create AT input block
//         // this.appendValueInput('NAME');
//         // connect shadow block
//         var cc = this.getInput('KEY').connection;
//         cc.connect(ob);
//     }
// };


// Blockly.JavaScript['insert_to_json_object'] = function(block) {
//     var variable = Blockly.JavaScript.variableDB_.getName(block.getFieldValue('VAR_NAME'), Blockly.Variables.NAME_TYPE);
//     var key = Blockly.JavaScript.valueToCode(block, 'KEY', Blockly.JavaScript.ORDER_ATOMIC);
//     var value = Blockly.JavaScript.valueToCode(block, 'VALUE', Blockly.JavaScript.ORDER_ATOMIC);
//     var code = '{"cmd": "insert_to_json_object", "variable": "' + variable + '", "key": ' + key + ', "value": ' + value + '},';
//     return code;
// };
