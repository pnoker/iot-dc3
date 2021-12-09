%% generic parser structure
-record(accept_option, {option,
                        q,
                        params}).

-type accept_option() :: #accept_option{}.

%% media range for 'Accept' field options
-record(media_range, {type,
                      subtype,
                      q,
                      params}).
-type media_range() :: #media_range{}.

%% content coding for 'Accept-Encoding' field options
-record(content_coding, {coding,
                         q,
                         params}).

-type content_coding() :: #content_coding{}.
