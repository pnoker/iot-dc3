%% This Source Code Form is subject to the terms of the Mozilla Public
%% License, v. 2.0. If a copy of the MPL was not distributed with this
%% file, You can obtain one at https://mozilla.org/MPL/2.0/.
%%
%% Copyright (c) 2019-2020 VMware, Inc. or its affiliates.  All rights reserved.
%%

-record(formatted_block, {lines = [] :: [stdout_formatter:formatted_line()],
                          props = #{width => 0,
                                    height => 0}
                          :: stdout_formatter:formatted_block_props()}).

-record(formatted_line, {content = "" :: unicode:chardata(),
                         props = #{width => 0,
                                   reformat_ok => false}
                         :: stdout_formatter:formatted_line_props()}).

-record(paragraph, {content :: term(),
                    props = #{} :: stdout_formatter:paragraph_props()}).

-record(cell, {content = #formatted_block{} :: stdout_formatter:formattable(),
               props = #{} :: stdout_formatter:cell_props()}).

-record(row, {cells = [] :: [stdout_formatter:formattable() | #cell{}],
              props = #{} :: stdout_formatter:row_props()}).

-record(table, {rows = [] :: [[stdout_formatter:formattable() | #cell{}] |
                              #row{}],
                props = #{} :: stdout_formatter:table_props()}).
