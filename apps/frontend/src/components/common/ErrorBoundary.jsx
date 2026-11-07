import React from 'react';

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught an error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: '60px 20px', textAlign: 'center', background: '#ffffff', minHeight: '60vh' }}>
          <h2 style={{ fontSize: '1.4rem', color: '#dc2626', marginBottom: '12px' }}>
            Đã xảy ra sự cố khi tải giao diện
          </h2>
          <p style={{ color: '#64748b', marginBottom: '20px', fontSize: '0.95rem' }}>
            {this.state.error?.message || 'Vui lòng thử tải lại trang hoặc quay về trang chủ.'}
          </p>
          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
            <button
              className="btn btn-primary btn-sm"
              onClick={() => this.setState({ hasError: false, error: null })}
            >
              Thử lại
            </button>
            <button
              className="btn btn-secondary btn-sm"
              onClick={() => window.location.reload()}
            >
              Tải lại trang web
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;

