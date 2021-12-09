{application,gun,
             [{description,"HTTP/1.1, HTTP/2 and Websocket client for Erlang/OTP."},
              {vsn,"1.3.3"},
              {modules, ['gun','gun_app','gun_content_handler','gun_data_h','gun_http','gun_http2','gun_sse_h','gun_sup','gun_tcp','gun_tls','gun_ws','gun_ws_h']},
              {registered,[gun_sup]},
              {applications,[kernel,stdlib,ssl,cowlib]},
              {mod,{gun_app,[]}},
              {env,[]}]}.
