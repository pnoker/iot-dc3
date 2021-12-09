-ifndef(etcd_hrl).
-define(etcd_hrl, true).
-include_lib("kernel/include/logger.hrl").

-define(HEADERS, [{<<"grpc-encoding">>, <<"identity">>}, {<<"content-type">>, <<"application/grpc+proto">>}]).
-define(GRPC_ERROR(Status, Message), {grpc_error, #{'grpc-status' => Status, 'grpc-message' => Message}}).

-export_type([key/0, value/0, context/0, name/0, grpc_status/0, eetcd_error/0]).
-type key() :: iodata().
-type value() :: iodata().
-type context() :: map().
-type name() :: atom() | reference().
-type eetcd_error() :: timeout|{grpc_error,grpc_status()}|{gun_down,any()}|{gun_conn_error,any()}|{gun_stream_error,any()}|eetcd_conn_unavailable.
-type grpc_status() :: #{'grpc-status' => integer(), 'grpc-message' => binary()}.

%% Grpc status code
-define(GRPC_STATUS_OK, 0).
-define(GRPC_STATUS_CANCELLED, 1).
-define(GRPC_STATUS_UNKNOWN, 2).
-define(GRPC_STATUS_INVALID_ARGUMENT, 3).
-define(GRPC_STATUS_DEADLINE_EXCEEDED, 4).
-define(GRPC_STATUS_NOT_FOUND, 5).
-define(GRPC_STATUS_ALREADY_EXISTS, 6).
-define(GRPC_STATUS_PERMISSION_DENIED, 7).
-define(GRPC_STATUS_RESOURCE_EXHAUSTED, 8).
-define(GRPC_STATUS_FAILED_PRECONDITION, 9).
-define(GRPC_STATUS_ABORTED, 10).
-define(GRPC_STATUS_OUT_OF_RANGE, 11).
-define(GRPC_STATUS_UNIMPLEMENTED, 12).
-define(GRPC_STATUS_INTERNAL, 13).
-define(GRPC_STATUS_UNAVAILABLE, 14).
-define(GRPC_STATUS_DATA_LOSS, 15).
-define(GRPC_STATUS_UNAUTHENTICATED, 16).

-define(ETCD_CONNS, eetcd_conns).
-record(eetcd_conn, {id, gun, conn, token = []}).
-endif.
