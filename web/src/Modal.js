import React from 'react';
import './Modal.css'

const Modal = ({show, onClose, isLogin, onFormSubmit}) => {
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
                    <div>
                        <p style={{
                            color: 'grey',
                            fontSize: '12px',
                            lineHeight: '25px'
                        }}>
                            提示：注册仅用于保存对话记录，不建议使用你在其它平台的密码，因为除了把你的真实密码加密一次之外，我的服务器没有任何安全措施。</p>
                        <p style={{
                            color: 'grey',
                            fontSize: '12px',
                            lineHeight: '25px'}}>
                            所以你可以随便输一个，反正我也没有限制密码格式，诶嘿。
                        </p>
                    </div>
                    <form onSubmit={handleSubmit}>
                        <input name="username" type="text" placeholder="用户名" required/>
                        <input name="password" type="password" placeholder="密码" required/>
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
