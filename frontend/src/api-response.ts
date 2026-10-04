function fallbackMessage(status: number): string {
  if (status === 401) return '登录已失效，请重新登录'
  if (status === 502 || status === 503) {
    return `后端服务暂时不可用，可能正在重启，请稍后重试（${status}）`
  }
  if (status === 504) return '服务器响应超时，请稍后重试（504）'
  return `请求失败（${status}）`
}

export async function readJsonResponse<T>(response: Response): Promise<T> {
  const text = await response.text()
  let data: unknown
  if (text.trim()) {
    try {
      data = JSON.parse(text)
    } catch {
      throw new Error(response.ok
        ? '服务器返回了非 JSON 内容，请刷新页面；若仍失败，请检查服务状态'
        : fallbackMessage(response.status))
    }
  }
  if (!response.ok) {
    const message = typeof data === 'object' && data !== null && 'message' in data
      && typeof data.message === 'string' ? data.message.trim() : ''
    throw new Error(message || fallbackMessage(response.status))
  }
  return data as T
}
