import React from 'react';
import './Modal.css'

const Modal = ({ show, onClose, isLogin, onFormSubmit }) => {
    if (!show) {
        return null;
    }

    const handleSubmit = (event) => {
        event.preventDefault();
        // Extract form data and call onFormSubmit
        const formData = new FormData(event.target);
        const data = Object.fromEntries(formData);
        onFormSubmit(data);
    };

    return (
        <div className="modal" onClick={onClose}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
                <div className="modal-header">
                    <h4 className="modal-title">{isLogin ? '登录' : '注册'}</h4>
                </div>
                <div className="modal-body">
                    <form onSubmit={handleSubmit}>
                        <input name="username" type="text" placeholder="用户名" required />
                        <input name="password" type="password" placeholder="密码" required />
                        <div className="modal-footer">
                            {isLogin ? (
                                <button type="submit">登录</button>
                            ) : (
                                <button type="submit">注册</button>
                            )}
                            <button onClick={onClose}>关闭</button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default Modal;
