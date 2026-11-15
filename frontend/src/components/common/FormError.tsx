/** Lỗi chung của một form: những lỗi không thuộc về ô nhập nào. Không có lỗi thì không hiện gì. */
export function FormError({ messages }: { messages: string[] }) {
  if (messages.length === 0) {
    return null
  }
  return (
    <div
      role="alert"
      className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive"
    >
      {messages.map((message) => (
        <p key={message}>{message}</p>
      ))}
    </div>
  )
}
