import { useEffect, useState } from 'react';
import {
  User,
  onAuthStateChanged,
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword,
  signOut,
} from 'firebase/auth';
import { auth } from '../firebase';

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => onAuthStateChanged(auth, (u) => {
    setUser(u);
    setLoading(false);
  }), []);

  return {
    user,
    loading,
    signUp: (email: string, password: string) => createUserWithEmailAndPassword(auth, email, password),
    signIn: (email: string, password: string) => signInWithEmailAndPassword(auth, email, password),
    logOut: () => signOut(auth),
  };
}
