/** RFC 9457 problem detail returned by the HTTP gateway. */
declare interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  code: string;
  detail: string;
  instance?: string;
  traceId?: string;
  errors?: Record<string, string[]>;
}
