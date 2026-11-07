import React, { createContext, useContext, useState, useEffect } from 'react';
import { cartApi } from '../api';
import { useAuth } from './AuthContext';
import { resolveProductImage } from '../images';

const CartContext = createContext(null);
const LOCAL_CART_KEY = 'brewlite_cart';

export const CartProvider = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const [cartItems, setCartItems] = useState(() => {
    try {
      const saved = localStorage.getItem(LOCAL_CART_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem(LOCAL_CART_KEY, JSON.stringify(cartItems));
    } catch {}
  }, [cartItems]);

  useEffect(() => {
    if (isAuthenticated && user?.id) {
      loadServerCart(user.id);
    }
  }, [isAuthenticated, user?.id]);

  const loadServerCart = async (customerId) => {
    try {
      const res = await cartApi.getCart(customerId);
      if (res && res.items) {
        setCartItems(res.items);
      }
    } catch {}
  };

  const addToCart = async (product, quantity = 1, size = 'M', note = '', selectedToppings = []) => {
    const sizeExtra = size === 'L' ? 10000 : size === 'M' ? 5000 : 0;
    const toppingsExtra = (selectedToppings || []).reduce((sum, t) => sum + (Number(t.unitPrice) || 0), 0);
    const basePrice = product.unitPrice || 0;
    const finalUnitPrice = basePrice + sizeExtra + toppingsExtra;

    const toppingsSummary = (selectedToppings || []).map((t) => t.name).sort().join(', ');

    const newItem = {
      id: Date.now(),
      productId: product.id,
      name: product.name,
      baseUnitPrice: basePrice,
      img: resolveProductImage(product),
      quantity,
      size,
      unitPrice: finalUnitPrice,
      note,
      toppings: selectedToppings || [],
      toppingsSummary,
      itemTotalPrice: finalUnitPrice * quantity,
    };

    setCartItems((prevItems) => {
      const existingIndex = prevItems.findIndex(
        (item) =>
          item.productId === product.id &&
          item.size === size &&
          item.note === note &&
          (item.toppingsSummary || '') === toppingsSummary
      );

      if (existingIndex > -1) {
        const updated = [...prevItems];
        const current = updated[existingIndex];
        const newQty = current.quantity + quantity;
        updated[existingIndex] = {
          ...current,
          quantity: newQty,
          itemTotalPrice: current.unitPrice * newQty,
        };
        return updated;
      }
      return [...prevItems, newItem];
    });

    if (isAuthenticated && user?.id) {
      try {
        await cartApi.addItem({
          customerId: user.id,
          item: {
            productId: product.id,
            quantity,
            size,
            note,
            toppings: (selectedToppings || []).map((t) => ({ toppingId: t.id, quantity: 1 })),
          },
        });
      } catch {}
    }
  };

  const updateQuantity = async (cartItemId, newQty) => {
    if (newQty <= 0) {
      removeFromCart(cartItemId);
      return;
    }

    setCartItems((prev) =>
      prev.map((item) =>
        item.id === cartItemId
          ? {
              ...item,
              quantity: newQty,
              itemTotalPrice: item.unitPrice * newQty,
            }
          : item
      )
    );

    if (isAuthenticated) {
      try {
        await cartApi.updateItemQuantity(cartItemId, newQty);
      } catch {}
    }
  };

  const removeFromCart = async (cartItemId) => {
    setCartItems((prev) => prev.filter((item) => item.id !== cartItemId));

    if (isAuthenticated) {
      try {
        await cartApi.removeItem(cartItemId);
      } catch {}
    }
  };

  const clearCart = async () => {
    setCartItems([]);
    if (isAuthenticated && user?.id) {
      try {
        await cartApi.clearCart(user.id);
      } catch {}
    }
  };

  const updateItem = (cartItemId, updates) => {
    setCartItems((prev) =>
      prev.map((item) => {
        if (item.id !== cartItemId) return item;
        const newSize = updates.size !== undefined ? updates.size : item.size;
        const newNote = updates.note !== undefined ? updates.note : item.note;
        const newQty = updates.quantity !== undefined ? updates.quantity : item.quantity;
        const newToppings = updates.toppings !== undefined ? updates.toppings : (item.toppings || []);
        const base =
          item.baseUnitPrice !== undefined
            ? item.baseUnitPrice
            : item.unitPrice - (item.size === 'L' ? 10000 : item.size === 'M' ? 5000 : 0);
        const sizeExtra = newSize === 'L' ? 10000 : newSize === 'M' ? 5000 : 0;
        const toppingsExtra = (newToppings || []).reduce((sum, t) => sum + (Number(t.price || t.unitPrice) || 0), 0);
        const newUnitPrice = Math.max(0, base + sizeExtra + toppingsExtra);
        return {
          ...item,
          size: newSize,
          note: newNote,
          toppings: newToppings,
          quantity: newQty,
          baseUnitPrice: base,
          unitPrice: newUnitPrice,
          itemTotalPrice: newUnitPrice * newQty,
        };
      })
    );
  };

  const totalItems = cartItems.reduce((acc, item) => acc + item.quantity, 0);
  const subtotal = cartItems.reduce(
    (acc, item) => acc + (item.itemTotalPrice || item.unitPrice * item.quantity),
    0
  );

  const value = {
    cartItems,
    totalItems,
    subtotal,
    addToCart,
    updateQuantity,
    updateItem,
    removeFromCart,
    clearCart,
  };

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

export default CartContext;
