var BLOCKLY_TOOLBOX_XML = BLOCKLY_TOOLBOX_XML || Object.create(null);


BLOCKLY_TOOLBOX_XML['standard'] = `
<xml id="toolbox" style="display: none">
    
    <category name="Controls" colour="290">
        <block type="for_loop"></block>
        <block type="for_loop_with_number"></block>
        <block type="if"></block>
        <block type="get_condition"></block>
        <block type="get_boolean"></block>
        <block type="function"></block>
        <block type="call_function"></block>
        <block type="break_or_continue"></block>
    </category>

    <category colour="330" name="Variables">
        <block type="set_variable"></block>
        <block type="get_variable"></block>
    </category>

    <category colour="20" name="Events">
        <block type="event_click"></block>
        <block type="event_wait_and_click"></block>
        <block type="type"></block>
        <block type="press_key"></block>
        <block type="wait_for"></block>
        <block type="wait_for_element"></block>
        <block type="scroll_bottom"></block>
        <block type="scroll_top"></block>
    </category>

    <category colour="65" name="HTML">
        <block type="get_html_elements"></block>
        <block type="get_attribute"></block>
        <block type="get_attributes"></block>
        <block type="get_text_content"></block>
        <block type="get_tag_name"></block>
        <block type="get_html_element_by_selector"></block>
        <block type="is_element_exist"></block>
        <block type="get_page_source"></block>
    </category>

    <category colour="160" name="Navigator">
        <block type="get_input"></block>
        <block type="got_to_website"></block>
        <block type="download_from_url"></block>
        <block type="write_to_a_file"></block>
        <block type="debug"></block>
        <block type="open_a_new_tab"></block>
        <block type="switch_tab"></block>
        <block type="close_tab"></block>
    </category>

    <category colour="230" name="Math">
        <block type="get_number"></block>
        <block type="get_calculation"></block>
    </category>

    <category colour="210" name="Text">
        <block type="get_text"></block>
        <block type="trim_space"></block>
        <block type="get_join_text"></block>
        <block type="append_text_to_var"></block>
        <block type="text_contains"></block>
        <block type="regex"></block>
    </category>

</xml>
`;
